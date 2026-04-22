from pii_utils import compliance_critical, ComplianceCategory


def test_decorator_is_transparent():
    @compliance_critical(category=ComplianceCategory.PII_HANDLING)
    def f(x):
        return x + 1

    assert f(1) == 2


def test_decorator_attaches_metadata():
    @compliance_critical(category="AUDIT_TRAIL", note="writes event log")
    def g():
        return "ok"

    meta = getattr(g, "__compliance_critical__", None)
    assert meta is not None
    assert meta["category"] == "AUDIT_TRAIL"
    assert meta["note"] == "writes event log"
