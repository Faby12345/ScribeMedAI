
import {ClinicalNotes} from "@/features/consultations/components/consultation-documentation-flow";
import {createHeaders, getCsrfToken} from "@/features/consultations/api/create-consultation";

export class SendNotesApiError extends Error {
    constructor(
        message: string,
        public readonly status?: number
    ) {
        super(message);
        this.name = "SendNotesApiError"
    }
}
/*
*  UUID notesId,
        UUID jobId,
        UUID consultationId,
        ProcessingJobStatus status*/

type SendNotesResponse = {
    notesId: string,
    jobId: string,
    consultationId: string,
    status: string
}



export async function sendNotes (
    consultationId: string,
    notes: ClinicalNotes
): Promise<SendNotesResponse> {
    let response :Response
    try {
        const csrfToken = await getCsrfToken()

        response = await fetch(`/api/v1/consultations/${consultationId}/notes`, {
            method: "POST",
            headers: createHeaders(csrfToken),
            credentials: "include",
            body: JSON.stringify( notes ),
        })
    } catch (e) {
        if(e instanceof SendNotesApiError){
            throw e
        }

        throw new SendNotesApiError(
             "Serverul nu este disponibil. Incearca din nou."
        )
    }
    if(response.status === 401 || response.status == 403){
        throw new SendNotesApiError(
            "Sessiunea a expirat sau cererea nu este autorizata",
            response.status,
        )
    }

    if(response.status === 400){
        throw new SendNotesApiError(
            "Notitele nu au putut fii trimise. Incearca din nou",
            response.status
        )
    }

    if(!response.ok) {
        throw new SendNotesApiError(
            "Notitele nu au putut fii trimise. Incearca din nou",
            response.status
        )
    }
    //console.log(response.json())
    return response.json();
}