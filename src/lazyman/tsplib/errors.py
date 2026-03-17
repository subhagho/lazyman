from __future__ import annotations


class TSPLibParseError(ValueError):
    def __init__(
        self,
        message: str,
        *,
        line_number: int | None = None,
        section: str | None = None,
    ) -> None:
        details = [message]
        if section is not None:
            details.append(f"section={section}")
        if line_number is not None:
            details.append(f"line={line_number}")
        super().__init__("; ".join(details))
        self.line_number = line_number
        self.section = section
