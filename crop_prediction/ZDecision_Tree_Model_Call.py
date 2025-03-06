import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import joblib
from collections import Counter
import cgitb
from flask import Flask, request, jsonify
from flask_cors import CORS  # Import CORS

cgitb.enable()

header = ['State_Name', 'District_Name', 'Season', 'Crop']

class Question:
    def __init__(self, column, value):
        self.column = column
        self.value = value
    
    def match(self, example):
        val = example[self.column]
        return val == self.value
    
    def match2(self, example):
        if example in ('True', 'true', '1'):
            return True
        return False
    
    def __repr__(self):
        return "Is %s %s %s?" % (header[self.column], "==", str(self.value))

def class_counts(Data):
    counts = {}
    for row in Data:
        label = row[-1]
        if label not in counts:
            counts[label] = 0
        counts[label] += 1
    return counts

class Leaf:
    def __init__(self, Data):
        self.predictions = class_counts(Data)

class Decision_Node:
    def __init__(self, question, true_branch, false_branch):
        self.question = question
        self.true_branch = true_branch
        self.false_branch = false_branch

def print_tree(node, spacing=""):
    if isinstance(node, Leaf):
        print(spacing + "Predict", node.predictions)
        return
    print(spacing + str(node.question))
    print(spacing + "--> True:")
    print_tree(node.true_branch, spacing + " ")
    print(spacing + "--> False:")
    print_tree(node.false_branch, spacing + " ")

def print_leaf(counts):
    total = sum(counts.values()) * 1.0
    probs = {}
    for lbl in counts.keys():
        probs[lbl] = str(int(counts[lbl] / total * 100)) + "%"
    return probs

def classify(row, node):
    if isinstance(node, Leaf):
        return node.predictions
    if node.question.match(row):
        return classify(row, node.true_branch)
    else:
        return classify(row, node.false_branch)

# Load the pre-trained decision tree model
dt_model_final = joblib.load('filetest2.pkl')

# Initialize Flask app
app = Flask(__name__)

# Disable CORS by allowing all origins
CORS(app, resources={r"/*": {"origins": "*"}})

# API endpoint to predict crop based on state, district, and season
@app.route('/predict', methods=['GET'])
def predict_crop():
    try:
        # Get parameters from query string
        state = request.args.get('state')
        district = request.args.get('district')
        season = request.args.get('season')

        # Validate inputs
        if not all([state, district, season]):
            return jsonify({
                'error': 'Missing required parameters: state, district, season'
            }), 400

        # Prepare testing data
        testing_data = [[state, district, season]]

        # Make prediction
        for row in testing_data:
            prediction_dict = print_leaf(classify(row, dt_model_final)).copy()

        # Return prediction as JSON response
        return jsonify({
            'state': state,
            'district': district,
            'season': season,
            'predictions': prediction_dict
        }), 200

    except Exception as e:
        return jsonify({
            'error': str(e)
        }), 500

# Alternative endpoint for POST request with JSON body (optional)
@app.route('/predict_post', methods=['POST'])
def predict_crop_post():
    try:
        # Get JSON data from request body
        data = request.get_json()
        state = data.get('state')
        district = data.get('district')
        season = data.get('season')

        # Validate inputs
        if not all([state, district, season]):
            return jsonify({
                'error': 'Missing required parameters: state, district, season'
            }), 400

        # Prepare testing data
        testing_data = [[state, district, season]]

        # Make prediction
        for row in testing_data:
            prediction_dict = print_leaf(classify(row, dt_model_final)).copy()

        # Return prediction as JSON response
        return jsonify({
            'state': state,
            'district': district,
            'season': season,
            'predictions': prediction_dict
        }), 200

    except Exception as e:
        return jsonify({
            'error': str(e)
        }), 500

if __name__ == '__main__':
    # Run Flask app
    app.run(debug=True, host='0.0.0.0', port=5001)