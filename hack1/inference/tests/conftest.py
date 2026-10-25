from unittest.mock import AsyncMock, patch
import pytest


@pytest.fixture(autouse=True)
def mock_gemini_in_tests(request):
    """
    Ensures unit tests outside test_gemini_explain do not make live outbound network calls
    even if GEMINI_API_KEY is present in the host environment.
    """
    if "test_gemini_explain" in request.node.nodeid:
        yield
        return

    mock_json = (
        '{"summary":"Synthetic test explanation","supportingEvidence":[],'
        '"limitations":[],"missingData":[],"recommendedHumanReview":[]}'
    )
    with patch(
        "app.routers.predict.generate_explanation",
        new=AsyncMock(return_value=mock_json),
    ):
        yield
