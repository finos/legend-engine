from deephaven import new_table
from deephaven.column import int_col, string_col, float_col, datetime_col, bool_col
from deephaven.time import to_j_instant

try:
    stockTrades = new_table([
        int_col("TradeID", [1, 2, 3, 4, 5, 6, 7, 8]),
        string_col("StockSymbol", ["AAPL", "GOOG", "AAPL", "MSFT", "GOOG", "TSLA", "AAPL", "MSFT"]),
        float_col("Price", [174.5, 142.3, 175.0, 423.1, 141.8, 248.9, 174.8, 422.5]),
        int_col("Quantity", [100, 50, 200, 75, 150, 300, 120, 90]),
        datetime_col("TradeTime", [
            to_j_instant("2025-03-06T09:30:00Z"),
            to_j_instant("2025-03-06T09:31:00Z"),
            to_j_instant("2025-03-06T09:32:00Z"),
            to_j_instant("2025-03-06T09:33:00Z"),
            to_j_instant("2025-03-06T09:34:00Z"),
            to_j_instant("2025-03-06T09:35:00Z"),
            to_j_instant("2025-03-06T09:36:00Z"),
            to_j_instant("2025-03-06T09:37:00Z")
        ]),
        string_col("City", ["New York", "London", "New York", "Tokyo", "London", "New York", "Tokyo", "New York"]),
        bool_col("IsBuy", [True, False, True, True, False, True, False, True])
    ])
    print("Table stockTrades created with financial dummy data")
except Exception as e:
    print(f"Error creating table: {e}")