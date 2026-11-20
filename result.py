from pathlib import Path
import matplotlib
matplotlib.use("Agg")  
import matplotlib.pyplot as plt
import pandas as pd


PROJECT_ROOT = Path(__file__).resolve().parent

RAW_FILE = PROJECT_ROOT / "cutoff_measurements.csv"
SUMMARY_DIR = PROJECT_ROOT
CHART_DIR = PROJECT_ROOT / "charts"


def load_measurements() -> pd.DataFrame:
    if not RAW_FILE.exists():
        raise FileNotFoundError(
            f"Cannot find {RAW_FILE}. Run CutoffTest.java first."
        )

    df = pd.read_csv(RAW_FILE)
    required = {"InputType", "N", "Cutoff", "Round", "ElapsedMs"}
    missing = required.difference(df.columns)
    if missing:
        raise ValueError(f"CSV is missing required columns: {sorted(missing)}")

    if df.empty:
        raise ValueError("The measurement CSV is empty.")

    if (df["ElapsedMs"] < 0).any():
        raise ValueError("ElapsedMs cannot contain negative values.")

    return df


def summarize(df: pd.DataFrame) -> pd.DataFrame:
    summary = (
        df.groupby(["InputType", "N", "Cutoff"], as_index=False)
        .agg(
            Repetitions=("ElapsedMs", "count"),
            MedianMs=("ElapsedMs", "median"),
            MeanMs=("ElapsedMs", "mean"),
            StdMs=("ElapsedMs", "std"),
            MinMs=("ElapsedMs", "min"),
            MaxMs=("ElapsedMs", "max"),
        )
        .sort_values(["InputType", "N", "Cutoff"])
    )
    return summary


def make_best_by_case(summary: pd.DataFrame) -> pd.DataFrame:
    indices = summary.groupby(["InputType", "N"])["MedianMs"].idxmin()
    best = summary.loc[indices].copy()
    best = best.sort_values(["InputType", "N"])
    return best


def make_recommendations(summary: pd.DataFrame) -> pd.DataFrame:
    """
    Compare cutoffs across different N values without letting the larger
    absolute runtimes dominate: for each (InputType, N), divide each
    cutoff's median by the minimum median in that case, then average ratios.
    Lower MeanRelativeTime is better.
    """
    work = summary.copy()
    case_min = work.groupby(["InputType", "N"])["MedianMs"].transform("min")
    work["RelativeTime"] = work["MedianMs"] / case_min

    recommendations = (
        work.groupby(["InputType", "Cutoff"], as_index=False)
        .agg(
            MeanRelativeTime=("RelativeTime", "mean"),
            CasesCompared=("RelativeTime", "count"),
        )
    )
    recommendations["Scope"] = recommendations["InputType"]
    recommendations = recommendations.drop(columns=["InputType"])

    overall = (
        work.groupby("Cutoff", as_index=False)
        .agg(
            MeanRelativeTime=("RelativeTime", "mean"),
            CasesCompared=("RelativeTime", "count"),
        )
    )
    overall["Scope"] = "ALL_TYPES"

    result = pd.concat([recommendations, overall], ignore_index=True)
    result = result[
        ["Scope", "Cutoff", "MeanRelativeTime", "CasesCompared"]
    ]
    return result.sort_values(["Scope", "MeanRelativeTime", "Cutoff"])


def create_charts(summary: pd.DataFrame) -> None:
    CHART_DIR.mkdir(parents=True, exist_ok=True)

    for (input_type, n), subset in summary.groupby(["InputType", "N"]):
        subset = subset.sort_values("Cutoff")
        best = subset.loc[subset["MedianMs"].idxmin()]

        fig, ax = plt.subplots(figsize=(9, 5))
        ax.plot(
            subset["Cutoff"],
            subset["MedianMs"],
            marker="o",
            markersize=3.5,
            linewidth=1.5,
            label="Median execution time",
        )
        ax.axvline(
            int(best["Cutoff"]),
            linestyle="--",
            label=(
                f"Best observed cutoff = {int(best['Cutoff'])} "
                f"({best['MedianMs']:.3f} ms)"
            ),
        )
        ax.set_title(
            f"Mergesort with Insertion Sort cutoff\n"
            f"Input type: {input_type}, N = {n:,}"
        )
        ax.set_xlabel("CUTOFF (subarray size)")
        ax.set_ylabel("Median execution time (ms)")
        ax.grid(True, linestyle="--", alpha=0.45)
        ax.legend()
        fig.tight_layout()

        filename = f"{input_type.lower()}_n_{n}.png"
        fig.savefig(CHART_DIR / filename, dpi=200)
        plt.close(fig)


def main() -> None:
    SUMMARY_DIR.mkdir(parents=True, exist_ok=True)
    CHART_DIR.mkdir(parents=True, exist_ok=True)

    measurements = load_measurements()
    summary = summarize(measurements)
    best = make_best_by_case(summary)
    recommendations = make_recommendations(summary)

    summary.to_csv(SUMMARY_DIR / "cutoff_summary.csv", index=False)
    best.to_csv(SUMMARY_DIR / "cutoff_best_by_case.csv", index=False)
    recommendations.to_csv(
        SUMMARY_DIR / "cutoff_recommendations.csv", index=False
    )

    create_charts(summary)

    print("\nBest observed cutoff for each input type and N:")
    print(
        best[["InputType", "N", "Cutoff", "MedianMs", "Repetitions"]]
        .to_string(index=False)
    )

    print("\nOverall recommendation across all input types and sizes:")
    overall = recommendations[
        recommendations["Scope"] == "ALL_TYPES"
    ].sort_values(["MeanRelativeTime", "Cutoff"])
    print(overall.head(10).to_string(index=False))

    print("\nGenerated files:")
    print(f"- {SUMMARY_DIR / 'cutoff_summary.csv'}")
    print(f"- {SUMMARY_DIR / 'cutoff_best_by_case.csv'}")
    print(f"- {SUMMARY_DIR / 'cutoff_recommendations.csv'}")
    print(f"- Charts: {CHART_DIR}")


if __name__ == "__main__":
    main()
