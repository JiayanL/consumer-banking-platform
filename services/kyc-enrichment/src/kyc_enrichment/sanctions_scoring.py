"""Detailed sanctions-match scoring.

The vendor returns a pre-computed match score, but compliance wants us
to re-score locally using a weighted blend of name / DOB / country so
we can defend the decision during exam. Not yet wired into the main
flow — the compliance team is still reviewing the weights.
"""
from __future__ import annotations

from dataclasses import dataclass
from difflib import SequenceMatcher
from typing import Iterable


@dataclass
class ScoringWeights:
    name: float = 0.6
    dob: float = 0.25
    country: float = 0.15


@dataclass
class SanctionsCandidate:
    list_name: str
    name: str
    dob: str
    country: str


def _name_score(a: str, b: str) -> float:
    if not a or not b:
        return 0.0
    return SequenceMatcher(None, a.lower(), b.lower()).ratio()


def _dob_score(a: str, b: str) -> float:
    if not a or not b:
        return 0.0
    if a == b:
        return 1.0
    # Allow one-year tolerance for transcription errors.
    try:
        ay, am, ad = [int(p) for p in a.split("-")]
        by, bm, bd = [int(p) for p in b.split("-")]
    except ValueError:
        return 0.0
    if (am, ad) == (bm, bd) and abs(ay - by) <= 1:
        return 0.75
    return 0.0


def _country_score(a: str, b: str) -> float:
    if not a or not b:
        return 0.0
    return 1.0 if a.upper() == b.upper() else 0.0


def score_candidate(
    subject_name: str,
    subject_dob: str,
    subject_country: str,
    candidate: SanctionsCandidate,
    weights: ScoringWeights | None = None,
) -> float:
    w = weights or ScoringWeights()
    total = (
        _name_score(subject_name, candidate.name) * w.name
        + _dob_score(subject_dob, candidate.dob) * w.dob
        + _country_score(subject_country, candidate.country) * w.country
    )
    return round(total, 4)


def best_match(
    subject_name: str,
    subject_dob: str,
    subject_country: str,
    candidates: Iterable[SanctionsCandidate],
    threshold: float = 0.7,
) -> SanctionsCandidate | None:
    best: tuple[float, SanctionsCandidate | None] = (0.0, None)
    for c in candidates:
        s = score_candidate(subject_name, subject_dob, subject_country, c)
        if s > best[0]:
            best = (s, c)
    if best[0] >= threshold:
        return best[1]
    return None
