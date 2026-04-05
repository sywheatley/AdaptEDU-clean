import sqlite3
import pandas as pd
import numpy as np
from sklearn.linear_model import LinearRegression
import matplotlib.pyplot as plt
import glob
import os

def analyze_effort_data(db_file):
    """
    Analyzes the effort estimation data from the JOSSE dataset to build a predictive model.

    Args:
        db_file (str): Path to the SQLite database file.
    """
    try:
        conn = sqlite3.connect(db_file)
        df = pd.read_sql_query('SELECT expert_estimated_effort, actual_effort FROM "case"', conn)
        conn.close()
    except Exception as e:
        print(f"Error reading database: {e}")
        print(f"Please ensure the database file '{db_file}' exists and is a valid SQLite file.")
        print("You may need to run 'python dataset_replication/csv_2_sqlite.py' first.")
        return

    print(f"Loaded {len(df)} records from the database.")

    # Jira's time tracking is in seconds. Convert to hours for readability.
    df['estimated_hours'] = df['expert_estimated_effort'] / 3600.0
    df['actual_hours'] = df['actual_effort'] / 3600.0

    # --- Data Cleaning ---
    # 1. Remove entries where estimate or actual is zero or less.
    original_count = len(df)
    df_cleaned = df[(df['estimated_hours'] > 0) & (df['actual_hours'] > 0)].copy()
    print(f"Removed {original_count - len(df_cleaned)} records with zero estimate or actual time.")

    # 2. Handle outliers by removing extreme ratios of actual/estimated time.
    df_cleaned['ratio'] = df_cleaned['actual_hours'] / df_cleaned['estimated_hours']
    p95 = df_cleaned['ratio'].quantile(0.95)
    original_count = len(df_cleaned)
    df_filtered = df_cleaned[df_cleaned['ratio'] <= p95].copy()
    print(f"Removed {original_count - len(df_filtered)} outlier records (ratio > {p95:.2f}).")
    print(f"Final dataset size for analysis: {len(df_filtered)} records.")

    if len(df_filtered) < 20:
        print("Not enough data left after cleaning. Cannot perform analysis.")
        return

    # --- Median Procrastination Multiplier Model ---
    # Instead of linear regression, we look at tasks that took LONGER than expected
    # to find a realistic "procrastination/underestimation" factor.
    
    # Filter for tasks that were underestimated
    underestimated_df = df_filtered[df_filtered['actual_hours'] > df_filtered['estimated_hours']]
    
    # Calculate the median multiplier
    slope = underestimated_df['ratio'].median()
    intercept = 0.0
    
    print("\n--- Median Procrastination Multiplier Model ---")
    print("This model isolates tasks that took longer than expected to find a typical multiplier.")
    print(f"\nEquation: actual_hours = {slope:.4f} * estimated_hours")
    print("\n==> Copy these values into main_alg.java <==")
    print(f"    private static final double EFFORT_SLOPE = {slope:.4f};")
    print(f"    private static final double EFFORT_INTERCEPT_HOURS = {intercept:.4f};")

    # --- Visualization ---
    plt.figure(figsize=(10, 6))
    plt.scatter(df_filtered['estimated_hours'], df_filtered['actual_hours'], alpha=0.1, label='Data points')
    
    # Plot the new multiplier line
    max_x = df_filtered['estimated_hours'].max()
    plt.plot([0, max_x], [0, max_x * slope], color='red', linewidth=2, label=f'Procrastination Multiplier ({slope:.2f}x)')
    plt.plot([0, df_filtered['estimated_hours'].max()], [0, df_filtered['estimated_hours'].max()], 'g--', label='Ideal (Estimate = Actual)')
    
    plt.title('JOSSE Dataset: Estimated vs. Actual Effort')
    plt.xlabel('Estimated Effort (hours)')
    plt.ylabel('Actual Effort (hours)')
    plt.legend()
    plt.grid(True, linestyle='--', alpha=0.6)
    plt.xlim(left=0, right=df_filtered['estimated_hours'].quantile(0.98))
    plt.ylim(bottom=0, top=df_filtered['actual_hours'].quantile(0.98))
    
    plot_filename = 'effort_analysis.png'
    plt.savefig(plot_filename)
    print(f"\nSaved analysis plot to '{plot_filename}'")


if __name__ == "__main__":
    # Find the sqlite file automatically
    db_files = glob.glob('JOSSE*.sqlite3') + glob.glob('dataset_replication/JOSSE*.sqlite3')
    if db_files:
        db_path = db_files[0]
        print(f"Found database file: {db_path}")
        analyze_effort_data(db_path)
    else:
        print("Could not find the JOSSE sqlite database file.")
        print("Please run 'python dataset_replication/csv_2_sqlite.py' first.")

       