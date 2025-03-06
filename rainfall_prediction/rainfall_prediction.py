import pandas as pd
from flask import Flask, request, jsonify
from flask_cors import CORS

# Initialize Flask app
app = Flask(__name__)
CORS(app, resources={r"/*": {"origins": "*"}})  # Disable CORS for development

# Load the dataset into a DataFrame
df = pd.read_csv('rainfall_in_india_1901-2015.csv')

# Define a function to predict rainfall for a given state and month
def predict_rainfall(state, month):
    # Convert inputs to uppercase to match typical CSV data formatting
    state = state.upper()
    month = month.upper()

    # Validate month
    valid_months = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC']
    if month not in valid_months:
        return None, f"Invalid month: {month}. Must be one of {valid_months}"

    # Filter the DataFrame to include only rows with the given state
    state_data = df[df['SUBDIVISION'] == state]

    if state_data.empty:
        return None, f"No data found for state: {state}"

    # Calculate the average rainfall for the given month across all years
    avg_rainfall = state_data[month].mean()

    # Handle NaN case
    if pd.isna(avg_rainfall):
        return None, f"No valid rainfall data for {state} in {month}"

    return avg_rainfall, None

# API endpoint to predict rainfall
@app.route('/predict_rainfall', methods=['GET'])
def predict_rainfall_api():
    try:
        # Get parameters from query string
        state = request.args.get('state')
        month = request.args.get('month')

        # Validate inputs
        if not state or not month:
            return jsonify({
                'error': 'Missing required parameters: state and month are required'
            }), 400

        # Predict rainfall
        rainfall, error = predict_rainfall(state, month)

        if error:
            return jsonify({'error': error}), 404

        # Return the predicted rainfall as JSON
        return jsonify({
            'state': state,
            'month': month,
            'predicted_rainfall': round(rainfall, 2)  # Round to 2 decimal places for readability
        }), 200

    except Exception as e:
        return jsonify({
            'error': f"Server error: {str(e)}"
        }), 500

if __name__ == '__main__':
    # Run Flask app
    app.run(debug=True, host='0.0.0.0', port=5000)