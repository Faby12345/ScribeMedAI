package ro.scribemed.backend.knowledge.application.chunk;

import org.springframework.stereotype.Service;
import ro.scribemed.backend.knowledge.dto.ExtractedPage;

import java.text.BreakIterator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ChunkingService {

    static final int DEFAULT_MAX_WORDS = 220;
    static final int DEFAULT_OVERLAP_WORDS = 40;

    private static final Locale ROMANIAN = Locale.forLanguageTag("ro");

    private final int maxWords;
    private final int overlapWords;

    public ChunkingService() {
        this(DEFAULT_MAX_WORDS, DEFAULT_OVERLAP_WORDS);
    }

    public ChunkingService(int maxWords, int overlapWords) {
        if (maxWords <= 0) {
            throw new IllegalArgumentException("maxWords must be greater than zero");
        }
        if (overlapWords < 0 || overlapWords >= maxWords) {
            throw new IllegalArgumentException(
                    "overlapWords must be between zero and maxWords - 1"
            );
        }

        this.maxWords = maxWords;
        this.overlapWords = overlapWords;
    }

    /**
     * Creates ordered, page-aware chunks from text extracted from a PDF.
     * Blank pages are ignored and the supplied list is not modified.
     */
    public List<KnowledgeChunkDraft> chunk(List<ExtractedPage> extractedPages) {
        Objects.requireNonNull(extractedPages, "extractedPages must not be null");

        List<TextUnit> units = extractedPages.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(ExtractedPage::page))
                .flatMap(page -> createTextUnits(page).stream())
                .toList();

        if (units.isEmpty()) {
            return List.of();
        }

        List<KnowledgeChunkDraft> chunks = new ArrayList<>();
        List<TextUnit> currentUnits = new ArrayList<>();
        int currentWordCount = 0;

        for (TextUnit unit : units) {
            if (!currentUnits.isEmpty()
                    && currentWordCount + unit.wordCount() > maxWords) {
                chunks.add(toChunk(chunks.size(), currentUnits));

                currentUnits = overlapFrom(currentUnits);
                currentWordCount = wordCount(currentUnits);

                while (!currentUnits.isEmpty()
                        && currentWordCount + unit.wordCount() > maxWords) {
                    TextUnit removed = currentUnits.removeFirst();
                    currentWordCount -= removed.wordCount();
                }
            }

            currentUnits.add(unit);
            currentWordCount += unit.wordCount();
        }

        if (!currentUnits.isEmpty()) {
            chunks.add(toChunk(chunks.size(), currentUnits));
        }
        return List.copyOf(chunks); // return the copy to make the result unmodifiable
    }

    private List<TextUnit> createTextUnits(ExtractedPage page) {
        if (page.content() == null || page.content().isBlank()) {
            return List.of();
        }

        String normalizedPage = normalize(page.content());
        String[] paragraphs = normalizedPage.split("\\n\\s*\\n");
        List<TextUnit> units = new ArrayList<>();

        for (int paragraphIndex = 0;
             paragraphIndex < paragraphs.length;
             paragraphIndex++) {
            String paragraph = paragraphs[paragraphIndex]
                    .replaceAll("\\s*\\n\\s*", " ")
                    .replaceAll("[\\p{Zs}\\t]+", " ")
                    .trim();

            if (paragraph.isEmpty()) {
                continue;
            }

            for (String sentence : splitSentences(paragraph)) {
                addSizedUnits(
                        units,
                        sentence,
                        page.page(),
                        paragraphIndex
                );
            }
        }

        return units;
    }

    private String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFC)
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("(?<=\\p{Ll})-\\n(?=\\p{Ll})", "")
                .strip();
    }

    private List<String> splitSentences(String paragraph) {
        BreakIterator iterator = BreakIterator.getSentenceInstance(ROMANIAN);
        iterator.setText(paragraph);

        List<String> sentences = new ArrayList<>();
        int start = iterator.first();

        for (int end = iterator.next();
             end != BreakIterator.DONE;
             start = end, end = iterator.next()) {
            String sentence = paragraph.substring(start, end).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }

        return sentences;
    }

    private void addSizedUnits(
            List<TextUnit> units,
            String text,
            int page,
            int paragraphIndex
    ) {
        List<String> words = words(text);

        for (int start = 0; start < words.size(); start += maxWords) {
            int end = Math.min(start + maxWords, words.size());
            String unitText = String.join(" ", words.subList(start, end));
            units.add(new TextUnit(
                    unitText,
                    page,
                    paragraphIndex,
                    end - start
            ));
        }
    }

    private List<TextUnit> overlapFrom(List<TextUnit> previousUnits) {
        if (overlapWords == 0) {
            return new ArrayList<>();
        }

        List<TextUnit> reversedOverlap = new ArrayList<>();
        int remainingWords = overlapWords;

        for (int index = previousUnits.size() - 1;
             index >= 0 && remainingWords > 0;
             index--) {
            TextUnit unit = previousUnits.get(index);

            if (unit.wordCount() <= remainingWords) {
                reversedOverlap.add(unit);
                remainingWords -= unit.wordCount();
                continue;
            }

            List<String> unitWords = words(unit.text());
            List<String> suffix = unitWords.subList(
                    unitWords.size() - remainingWords,
                    unitWords.size()
            );
            reversedOverlap.add(new TextUnit(
                    String.join(" ", suffix),
                    unit.page(),
                    unit.paragraphIndex(),
                    remainingWords
            ));
            remainingWords = 0;
        }

        List<TextUnit> overlap = new ArrayList<>(reversedOverlap.size());
        for (int index = reversedOverlap.size() - 1; index >= 0; index--) {
            overlap.add(reversedOverlap.get(index));
        }
        return overlap;
    }

    private KnowledgeChunkDraft toChunk(int index, List<TextUnit> units) {
        StringBuilder content = new StringBuilder();
        TextUnit previous = null;

        for (TextUnit unit : units) {
            if (!content.isEmpty()) {
                boolean newParagraph = previous.page() != unit.page()
                        || previous.paragraphIndex() != unit.paragraphIndex();
                content.append(newParagraph ? "\n\n" : " ");
            }
            content.append(unit.text());
            previous = unit;
        }

        int pageFrom = units.stream()
                .mapToInt(TextUnit::page)
                .min()
                .orElseThrow();
        int pageTo = units.stream()
                .mapToInt(TextUnit::page)
                .max()
                .orElseThrow();

        return new KnowledgeChunkDraft(
                index,
                content.toString(),
                pageFrom,
                pageTo
        );
    }

    private int wordCount(List<TextUnit> units) {
        return units.stream().mapToInt(TextUnit::wordCount).sum();
    }

    private List<String> words(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return List.of(text.trim().split("\\s+"));
    }


    private record TextUnit(
            String text,
            int page,
            int paragraphIndex,
            int wordCount
    ) {
    }
}
