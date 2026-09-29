import type { InputHTMLAttributes } from "react";

import { Input } from "@/components/ui/input";

type SearchInputProps = Omit<InputHTMLAttributes<HTMLInputElement>, "type">;

export function SearchInput(props: SearchInputProps) {
  return <Input type="search" {...props} />;
}
