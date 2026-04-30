"""Tests for sanctions_scoring module.

Covers the local re-scoring logic used to defend sanctions-match
decisions during OCC exam. OCC category: PII_HANDLING.
Jira: KAN-5
"""
import pytest

from kyc_enrichment.sanctions_scoring import (
    SanctionsCandidate,
    ScoringWeights,
    _country_score,
    _dob_score,
    _name_score,
    best_match,
    score_candidate,
)


# ── _name_score ──────────────────────────────────────────────────────


class TestNameScore:
    def test_exact_match_returns_one(self):
        assert _name_score("John Doe", "John Doe") == 1.0

    def test_case_insensitive(self):
        assert _name_score("john doe", "JOHN DOE") == 1.0

    def test_partial_match_returns_fraction(self):
        score = _name_score("John Doe", "Jon Doe")
        assert 0.0 < score < 1.0

    def test_completely_different_names(self):
        score = _name_score("Alice Smith", "Xyz Abc")
        assert score < 0.5

    def test_empty_first_returns_zero(self):
        assert _name_score("", "John") == 0.0

    def test_empty_second_returns_zero(self):
        assert _name_score("John", "") == 0.0

    def test_both_empty_returns_zero(self):
        assert _name_score("", "") == 0.0


# ── _dob_score ───────────────────────────────────────────────────────


class TestDobScore:
    def test_exact_match_returns_one(self):
        assert _dob_score("1990-01-15", "1990-01-15") == 1.0

    def test_one_year_tolerance_returns_075(self):
        assert _dob_score("1990-01-15", "1991-01-15") == 0.75

    def test_one_year_behind_tolerance_returns_075(self):
        assert _dob_score("1991-01-15", "1990-01-15") == 0.75

    def test_two_year_gap_returns_zero(self):
        assert _dob_score("1990-01-15", "1992-01-15") == 0.0

    def test_same_year_different_month_returns_zero(self):
        assert _dob_score("1990-01-15", "1990-02-15") == 0.0

    def test_same_year_different_day_returns_zero(self):
        assert _dob_score("1990-01-15", "1990-01-16") == 0.0

    def test_empty_first_returns_zero(self):
        assert _dob_score("", "1990-01-15") == 0.0

    def test_empty_second_returns_zero(self):
        assert _dob_score("1990-01-15", "") == 0.0

    def test_both_empty_returns_zero(self):
        assert _dob_score("", "") == 0.0

    def test_invalid_format_returns_zero(self):
        assert _dob_score("not-a-date", "1990-01-15") == 0.0

    def test_invalid_both_returns_zero(self):
        assert _dob_score("abc", "xyz") == 0.0


# ── _country_score ───────────────────────────────────────────────────


class TestCountryScore:
    def test_exact_match_returns_one(self):
        assert _country_score("US", "US") == 1.0

    def test_case_insensitive_match(self):
        assert _country_score("us", "US") == 1.0

    def test_mixed_case(self):
        assert _country_score("Us", "uS") == 1.0

    def test_different_countries_returns_zero(self):
        assert _country_score("US", "GB") == 0.0

    def test_empty_first_returns_zero(self):
        assert _country_score("", "US") == 0.0

    def test_empty_second_returns_zero(self):
        assert _country_score("US", "") == 0.0

    def test_both_empty_returns_zero(self):
        assert _country_score("", "") == 0.0


# ── score_candidate ──────────────────────────────────────────────────


class TestScoreCandidate:
    @pytest.fixture()
    def candidate(self):
        return SanctionsCandidate(
            list_name="OFAC-SDN",
            name="John Doe",
            dob="1990-01-15",
            country="US",
        )

    def test_perfect_match_with_default_weights(self, candidate):
        score = score_candidate("John Doe", "1990-01-15", "US", candidate)
        assert score == 1.0

    def test_name_only_match(self, candidate):
        score = score_candidate("John Doe", "2000-06-01", "GB", candidate)
        expected = round(1.0 * 0.6 + 0.0 * 0.25 + 0.0 * 0.15, 4)
        assert score == expected

    def test_dob_only_match(self, candidate):
        score = score_candidate("Xyz Abc", "1990-01-15", "GB", candidate)
        name_part = _name_score("Xyz Abc", "John Doe") * 0.6
        expected = round(name_part + 1.0 * 0.25 + 0.0 * 0.15, 4)
        assert score == expected

    def test_country_only_match(self, candidate):
        score = score_candidate("Xyz Abc", "2000-06-01", "US", candidate)
        name_part = _name_score("Xyz Abc", "John Doe") * 0.6
        expected = round(name_part + 0.0 * 0.25 + 1.0 * 0.15, 4)
        assert score == expected

    def test_custom_weights(self, candidate):
        w = ScoringWeights(name=0.5, dob=0.3, country=0.2)
        score = score_candidate("John Doe", "1990-01-15", "US", candidate, weights=w)
        assert score == 1.0

    def test_custom_weights_name_heavy(self, candidate):
        w = ScoringWeights(name=1.0, dob=0.0, country=0.0)
        score = score_candidate("John Doe", "2000-01-01", "GB", candidate, weights=w)
        assert score == 1.0

    def test_no_match_returns_low_score(self):
        candidate = SanctionsCandidate(
            list_name="OFAC-SDN",
            name="Completely Different",
            dob="1950-12-25",
            country="IR",
        )
        score = score_candidate("John Doe", "1990-01-15", "US", candidate)
        assert score < 0.5

    def test_dob_near_miss_contributes_partial(self, candidate):
        score = score_candidate("John Doe", "1991-01-15", "US", candidate)
        expected = round(1.0 * 0.6 + 0.75 * 0.25 + 1.0 * 0.15, 4)
        assert score == expected

    def test_result_is_rounded_to_four_decimals(self):
        candidate = SanctionsCandidate(
            list_name="SDN", name="Ab", dob="2000-01-01", country="XX",
        )
        score = score_candidate("Abc", "2000-01-01", "XX", candidate)
        assert isinstance(score, float)
        parts = str(score).split(".")
        assert len(parts) <= 2
        if len(parts) == 2:
            assert len(parts[1]) <= 4


# ── best_match ───────────────────────────────────────────────────────


class TestBestMatch:
    def _make_candidate(self, name, dob, country, list_name="OFAC-SDN"):
        return SanctionsCandidate(
            list_name=list_name, name=name, dob=dob, country=country,
        )

    def test_returns_best_above_threshold(self):
        candidates = [
            self._make_candidate("John Doe", "1990-01-15", "US"),
            self._make_candidate("Jane Smith", "1985-03-20", "GB"),
        ]
        result = best_match("John Doe", "1990-01-15", "US", candidates)
        assert result is not None
        assert result.name == "John Doe"

    def test_returns_none_when_all_below_threshold(self):
        candidates = [
            self._make_candidate("Xyz Abc", "1950-12-25", "IR"),
            self._make_candidate("Qqq Rrr", "1960-06-06", "CN"),
        ]
        result = best_match("John Doe", "1990-01-15", "US", candidates)
        assert result is None

    def test_returns_none_for_empty_candidates(self):
        result = best_match("John Doe", "1990-01-15", "US", [])
        assert result is None

    def test_custom_threshold_lower(self):
        candidates = [
            self._make_candidate("Jon Doe", "1990-01-15", "US"),
        ]
        result = best_match("John Doe", "1990-01-15", "US", candidates, threshold=0.5)
        assert result is not None

    def test_custom_threshold_higher_filters_out(self):
        candidates = [
            self._make_candidate("Jon Doe", "1991-01-15", "GB"),
        ]
        result = best_match("John Doe", "1990-01-15", "US", candidates, threshold=0.99)
        assert result is None

    def test_picks_highest_scorer_among_multiple(self):
        candidates = [
            self._make_candidate("Jon Doe", "1990-01-15", "US"),
            self._make_candidate("John Doe", "1990-01-15", "US"),
        ]
        result = best_match("John Doe", "1990-01-15", "US", candidates)
        assert result is not None
        assert result.name == "John Doe"

    def test_threshold_boundary_exact_equal(self):
        candidate = self._make_candidate("John Doe", "1990-01-15", "US")
        score = score_candidate("John Doe", "1990-01-15", "US", candidate)
        result = best_match(
            "John Doe", "1990-01-15", "US", [candidate], threshold=score,
        )
        assert result is not None

    def test_threshold_boundary_just_above(self):
        candidate = self._make_candidate("John Doe", "1990-01-15", "US")
        score = score_candidate("John Doe", "1990-01-15", "US", candidate)
        result = best_match(
            "John Doe", "1990-01-15", "US", [candidate], threshold=score + 0.0001,
        )
        assert result is None

    def test_generator_input(self):
        """best_match accepts any Iterable, including generators."""

        def gen():
            yield self._make_candidate("John Doe", "1990-01-15", "US")

        result = best_match("John Doe", "1990-01-15", "US", gen())
        assert result is not None


# ── dataclass defaults ───────────────────────────────────────────────


class TestScoringWeights:
    def test_default_weights_sum_to_one(self):
        w = ScoringWeights()
        assert w.name + w.dob + w.country == pytest.approx(1.0)

    def test_default_values(self):
        w = ScoringWeights()
        assert w.name == 0.6
        assert w.dob == 0.25
        assert w.country == 0.15

    def test_custom_weights(self):
        w = ScoringWeights(name=0.5, dob=0.3, country=0.2)
        assert w.name == 0.5
        assert w.dob == 0.3
        assert w.country == 0.2
