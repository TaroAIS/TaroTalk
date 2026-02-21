from typing import Any, Dict, List, Optional, Tuple


class SafetyLinter:
    HARD_BLOCK = "SAFETY_BLOCKED"
    SOFT_WARNING = "SOFT_WARNING"
    ALLOWED_RELATIONSHIP_TYPES = {"friend", "mentor", "rival", "advertiser", "self-agent"}
    DANGEROUS_PATTERNS = ["<script", "drop table", "union select", "../", "javascript:"]
    MAX_CONTENT_LEN = 800
    MAX_PAYLOAD_STRING_LEN = 2000

    def lint_tool_call(self, name: Optional[str], arguments: Any, world_id: Optional[str]) -> List[Dict[str, Any]]:
        reports: List[Dict[str, Any]] = []
        tool_name = str(name or "").strip()
        if not tool_name:
            reports.append(
                self._report(
                    scope="tool_call",
                    rule="TOOL_NAME_REQUIRED",
                    severity="ERROR",
                    action=self.HARD_BLOCK,
                    reason="tool name is required",
                )
            )
            return reports

        if not isinstance(arguments, dict):
            reports.append(
                self._report(
                    scope="tool_call",
                    rule="TOOL_ARGUMENT_TYPE",
                    severity="ERROR",
                    action=self.HARD_BLOCK,
                    reason="tool arguments must be object",
                    metadata={"tool_name": tool_name},
                )
            )
            return reports

        reports.extend(self._lint_payload(arguments, f"tool:{tool_name}"))

        if world_id:
            arg_world_id = str(arguments.get("world_id") or "").strip()
            if arg_world_id and arg_world_id != str(world_id):
                reports.append(
                    self._report(
                        scope="tool_call",
                        rule="WORLD_BOUNDARY_MISMATCH",
                        severity="ERROR",
                        action=self.HARD_BLOCK,
                        reason="tool world_id does not match request world_id",
                        metadata={"tool_name": tool_name, "tool_world_id": arg_world_id, "request_world_id": world_id},
                    )
                )

        if tool_name == "update_relationship":
            rel_type = str(arguments.get("type") or "").strip().lower()
            if rel_type not in self.ALLOWED_RELATIONSHIP_TYPES:
                reports.append(
                    self._report(
                        scope="tool_call",
                        rule="RELATIONSHIP_TYPE_WHITELIST",
                        severity="ERROR",
                        action=self.HARD_BLOCK,
                        reason="relationship type not allowed",
                        metadata={"tool_name": tool_name, "type": rel_type},
                    )
                )

        for key in ("content", "title"):
            value = arguments.get(key)
            if isinstance(value, str) and len(value) > self.MAX_CONTENT_LEN:
                reports.append(
                    self._report(
                        scope="tool_call",
                        rule="PAYLOAD_LENGTH_HIGH",
                        severity="WARN",
                        action=self.SOFT_WARNING,
                        reason=f"{key} length exceeds soft threshold",
                        metadata={"tool_name": tool_name, "length": len(value), "field": key},
                    )
                )

        return reports

    def filter_state_effects(
        self, effects: List[Dict[str, Any]], world_id: Optional[str]
    ) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]]]:
        reports: List[Dict[str, Any]] = []
        safe_effects: List[Dict[str, Any]] = []
        for effect in effects:
            effect_reports = self.lint_state_effect(effect, world_id)
            reports.extend(effect_reports)
            blocked = any(item.get("action") == self.HARD_BLOCK for item in effect_reports)
            if not blocked:
                safe_effects.append(effect)
        return safe_effects, reports

    def lint_state_effect(self, effect: Dict[str, Any], world_id: Optional[str]) -> List[Dict[str, Any]]:
        reports: List[Dict[str, Any]] = []
        if not isinstance(effect, dict):
            reports.append(
                self._report(
                    scope="state_effect",
                    rule="EFFECT_TYPE",
                    severity="ERROR",
                    action=self.HARD_BLOCK,
                    reason="state effect must be object",
                )
            )
            return reports

        effect_type = str(effect.get("effect_type") or "").strip()
        if not effect_type:
            reports.append(
                self._report(
                    scope="state_effect",
                    rule="EFFECT_TYPE_REQUIRED",
                    severity="ERROR",
                    action=self.HARD_BLOCK,
                    reason="effect_type is required",
                )
            )

        effect_world_id = str(effect.get("world_id") or "").strip()
        if world_id and effect_world_id and effect_world_id != str(world_id):
            reports.append(
                self._report(
                    scope="state_effect",
                    rule="WORLD_BOUNDARY_MISMATCH",
                    severity="ERROR",
                    action=self.HARD_BLOCK,
                    reason="state effect world_id does not match request world_id",
                    metadata={"effect_world_id": effect_world_id, "request_world_id": world_id},
                )
            )

        content = effect.get("content")
        if isinstance(content, str):
            if len(content) > self.MAX_PAYLOAD_STRING_LEN:
                reports.append(
                    self._report(
                        scope="state_effect",
                        rule="CONTENT_TOO_LARGE",
                        severity="ERROR",
                        action=self.HARD_BLOCK,
                        reason="content exceeds hard limit",
                        metadata={"length": len(content)},
                    )
                )
            elif len(content) > self.MAX_CONTENT_LEN:
                reports.append(
                    self._report(
                        scope="state_effect",
                        rule="CONTENT_LENGTH_HIGH",
                        severity="WARN",
                        action=self.SOFT_WARNING,
                        reason="content exceeds soft threshold",
                        metadata={"length": len(content)},
                    )
                )

        reports.extend(self._lint_payload(effect, "state_effect"))

        if effect_type == "TOOL_CALL":
            reports.extend(self.lint_tool_call(effect.get("tool_name"), effect.get("arguments") or {}, world_id))
        return reports

    def _lint_payload(self, payload: Any, scope_tag: str) -> List[Dict[str, Any]]:
        reports: List[Dict[str, Any]] = []
        for key, value in self._iter_payload(payload):
            if not isinstance(value, str):
                continue
            normalized = value.lower()
            if len(value) > self.MAX_PAYLOAD_STRING_LEN:
                reports.append(
                    self._report(
                        scope="payload",
                        rule="PAYLOAD_TOO_LARGE",
                        severity="ERROR",
                        action=self.HARD_BLOCK,
                        reason="payload field exceeds hard limit",
                        metadata={"field": key, "scope": scope_tag, "length": len(value)},
                    )
                )
            for pattern in self.DANGEROUS_PATTERNS:
                if pattern in normalized:
                    reports.append(
                        self._report(
                            scope="payload",
                            rule="PAYLOAD_INJECTION_PATTERN",
                            severity="ERROR",
                            action=self.HARD_BLOCK,
                            reason=f"dangerous pattern detected: {pattern}",
                            metadata={"field": key, "scope": scope_tag},
                        )
                    )
                    break
        return reports

    def _iter_payload(self, payload: Any, prefix: str = ""):
        if isinstance(payload, dict):
            for key, value in payload.items():
                next_prefix = f"{prefix}.{key}" if prefix else str(key)
                if isinstance(value, (dict, list)):
                    for row in self._iter_payload(value, next_prefix):
                        yield row
                else:
                    yield next_prefix, value
        elif isinstance(payload, list):
            for idx, value in enumerate(payload):
                next_prefix = f"{prefix}[{idx}]"
                if isinstance(value, (dict, list)):
                    for row in self._iter_payload(value, next_prefix):
                        yield row
                else:
                    yield next_prefix, value

    def _report(
        self,
        scope: str,
        rule: str,
        severity: str,
        action: str,
        reason: str,
        metadata: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        return {
            "scope": scope,
            "rule": rule,
            "severity": severity,
            "action": action,
            "reason": reason,
            "metadata": metadata or {},
        }
