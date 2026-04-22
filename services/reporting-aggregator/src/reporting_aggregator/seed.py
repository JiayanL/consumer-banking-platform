"""In-memory seed of fake transactions used by the aggregator.

Real deployments point at the ledger-service read replica; in dev and
tests we work off this fixed list.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable


@dataclass(frozen=True)
class Transaction:
    txn_id: str
    account_id: str
    posted_date: str  # YYYY-MM-DD
    type: str  # DEPOSIT | WITHDRAWAL | TRANSFER | FEE
    amount: float  # dollars


TRANSACTIONS: tuple[Transaction, ...] = (
    Transaction("t-0001", "acct-a", "2024-03-01", "DEPOSIT", 1_250.00),
    Transaction("t-0002", "acct-a", "2024-03-01", "WITHDRAWAL", 40.00),
    Transaction("t-0003", "acct-b", "2024-03-01", "DEPOSIT", 500.00),
    Transaction("t-0004", "acct-b", "2024-03-01", "FEE", 12.00),
    Transaction("t-0005", "acct-c", "2024-03-01", "DEPOSIT", 15_000.00),  # flagged
    Transaction("t-0006", "acct-a", "2024-03-02", "TRANSFER", 200.00),
    Transaction("t-0007", "acct-b", "2024-03-02", "DEPOSIT", 75.00),
    Transaction("t-0008", "acct-c", "2024-03-02", "WITHDRAWAL", 12_500.00),  # flagged
    Transaction("t-0009", "acct-d", "2024-03-03", "DEPOSIT", 9_999.00),
    Transaction("t-0010", "acct-d", "2024-03-15", "DEPOSIT", 250.00),
    Transaction("t-0011", "acct-a", "2024-04-01", "DEPOSIT", 2_000.00),
    Transaction("t-0012", "acct-b", "2024-04-02", "WITHDRAWAL", 300.00),
    Transaction("t-0013", "acct-c", "2024-04-05", "DEPOSIT", 10_001.00),  # flagged
)


def for_date(date: str) -> Iterable[Transaction]:
    return [t for t in TRANSACTIONS if t.posted_date == date]


def for_month(year_month: str) -> Iterable[Transaction]:
    return [t for t in TRANSACTIONS if t.posted_date.startswith(year_month)]
