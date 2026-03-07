import os

from flask import Flask, jsonify, render_template, request

from etree.params import GrowthParams
from etree.simulation import run_simulation

app = Flask(
    __name__,
    template_folder=os.path.join(os.path.dirname(__file__), 'templates'),
    static_folder=os.path.join(os.path.dirname(__file__), 'static'),
)


@app.route('/')
def index():
    return render_template('index.html')


@app.route('/api/params')
def get_params():
    return jsonify(GrowthParams.slider_metadata())


@app.route('/api/simulate', methods=['POST'])
def simulate():
    data = request.get_json(force=True)
    params = GrowthParams(**{k: type(getattr(GrowthParams, k, 0))(v)
                             for k, v in data.items()
                             if hasattr(GrowthParams, k)})
    result = run_simulation(params)
    return jsonify(result)
