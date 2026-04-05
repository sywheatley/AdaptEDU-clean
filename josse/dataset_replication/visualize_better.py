import sqlite3
import pandas as pd
import matplotlib.pyplot as plt
import glob

def create_better_visualizations():
    # Find the database
    db_files = glob.glob('JOSSE*.sqlite3') + glob.glob('dataset_replication/JOSSE*.sqlite3')
    if not db_files:
        print("Could not find the JOSSE sqlite database file.")
        return

    # Load and clean data
    conn = sqlite3.connect(db_files[0])
    df = pd.read_sql_query('SELECT expert_estimated_effort, actual_effort FROM "case"', conn)
    conn.close()

    df['estimated_hours'] = df['expert_estimated_effort'] / 3600.0
    df['actual_hours'] = df['actual_effort'] / 3600.0
    df = df[(df['estimated_hours'] > 0) & (df['actual_hours'] > 0)].copy()

    # Create visualization figure with 2 subplots
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(16, 6))

    # --- Plot 1: Log-Log Scatter Plot ---
    # A log scale helps us see the dense clusters of data without the extreme 100+ hour outliers ruining the scale
    ax1.scatter(df['estimated_hours'], df['actual_hours'], alpha=0.1, color='blue')
    ax1.plot([0.1, 1000], [0.1, 1000], 'g--', label='Ideal (Estimate = Actual)')
    ax1.set_xscale('log')
    ax1.set_yscale('log')
    ax1.set_title('Log-Log Scatter: Estimated vs Actual')
    ax1.set_xlabel('Estimated Effort (Hours) - Log Scale')
    ax1.set_ylabel('Actual Effort (Hours) - Log Scale')
    ax1.legend()

    # --- Plot 2: Boxplots by Estimate Buckets ---
    # Group the estimates into buckets to see the distribution of actual times for each group
    bins = [0, 1, 2, 4, 8, 16, 40, 100]
    labels = ['<1h', '1-2h', '2-4h', '4-8h', '8-16h', '16-40h', '40-100h']
    df['estimate_bucket'] = pd.cut(df['estimated_hours'], bins=bins, labels=labels)
    
    # showfliers=False hides the extreme outliers so we can actually see the boxes
    df.boxplot(column='actual_hours', by='estimate_bucket', ax=ax2, showfliers=False, patch_artist=True)
    ax2.set_title('Distribution of Actual Time Spent per Estimate Bucket')
    ax2.set_xlabel('What the User Estimated')
    ax2.set_ylabel('Actual Hours Spent (Outliers Hidden)')
    plt.suptitle('') # Remove pandas default title

    plt.tight_layout()
    plt.savefig('better_visualization.png', dpi=300)
    print("Saved deep-dive visualization to 'better_visualization.png'")

if __name__ == "__main__":
    create_better_visualizations()