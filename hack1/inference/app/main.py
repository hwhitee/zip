"""
WardSentinel Inference Service – FastAPI application entry point.

DISCLAIMER: This service produces heuristic DEMONSTRATION scores from synthetic
ambient sensor data. It is NOT clinically validated and must NOT be used for
real clinical decision-making.
"""

from __future__ import annotations

import logging
from datetime import datetime, timezone

from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.models.schemas import ApiError
from app.routers.predict import router as predict_router

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-8s %(name)s – %(message)s",
)
logger = logging.getLogger(__name__)

# ─────────────────────────────────────────────────────────────────────────────
#  Application
# ─────────────────────────────────────────────────────────────────────────────

app = FastAPI(
    title="WardSentinel Inference Service",
    description=(
        "Heuristic ambient-sensor scoring engine for predictive ward safety.\n\n"
        "**DISCLAIMER:** All scores are demonstration values derived from "
        "synthetic sensor data using hand-tuned heuristic rules. "
        "They are NOT calibrated clinical probabilities and have NOT "
        "been clinically validated. This service must NOT be used for "
        "real clinical decision-making."
    ),
    version="0.1.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

app.include_router(predict_router)


# ─────────────────────────────────────────────────────────────────────────────
#  Uniform error handling (matches ApiError contract)
# ─────────────────────────────────────────────────────────────────────────────


def _now_iso() -> str:
    return datetime.now(tz=timezone.utc).isoformat()


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(
    request: Request, exc: RequestValidationError
) -> JSONResponse:
    """Translate Pydantic validation errors into the uniform ApiError envelope."""
    # Flatten all errors into a single readable message
    messages = []
    for err in exc.errors():
        loc = " → ".join(str(p) for p in err["loc"])
        messages.append(f"{loc}: {err['msg']}")

    error_body = ApiError(
        timestamp=_now_iso(),
        status=status.HTTP_422_UNPROCESSABLE_ENTITY,
        error="Unprocessable Entity",
        message="; ".join(messages),
        path=str(request.url.path),
    )
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content=error_body.model_dump(),
    )


@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    logger.exception("Unhandled exception on %s", request.url.path)
    error_body = ApiError(
        timestamp=_now_iso(),
        status=status.HTTP_500_INTERNAL_SERVER_ERROR,
        error="Internal Server Error",
        message="An unexpected error occurred. See server logs.",
        path=str(request.url.path),
    )
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content=error_body.model_dump(),
    )


# ─────────────────────────────────────────────────────────────────────────────
#  Startup banner
# ─────────────────────────────────────────────────────────────────────────────


@app.on_event("startup")
async def _startup() -> None:
    logger.info("=" * 60)
    logger.info("WardSentinel Inference Service v0.1.0-heuristic")
    logger.info("DEMO ONLY – NOT FOR CLINICAL USE")
    logger.info("=" * 60)
    logger.info("Endpoints: POST /predict | GET /health | GET /model/info")
