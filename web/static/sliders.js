function buildSliders(container, metadata, onChange) {
    const inputs = {};

    metadata.forEach(param => {
        const group = document.createElement('div');
        group.className = 'slider-group';

        const labelRow = document.createElement('div');
        labelRow.className = 'slider-label-row';

        const label = document.createElement('label');
        label.innerHTML = `${param.label} <span class="value">${param.default}</span>`;

        labelRow.appendChild(label);

        if (param.description) {
            const helpBtn = document.createElement('button');
            helpBtn.className = 'help-btn';
            helpBtn.textContent = '?';
            helpBtn.type = 'button';

            const desc = document.createElement('p');
            desc.className = 'slider-desc';
            desc.textContent = param.description;
            desc.style.display = 'none';

            helpBtn.addEventListener('click', () => {
                const visible = desc.style.display !== 'none';
                desc.style.display = visible ? 'none' : 'block';
            });

            labelRow.appendChild(helpBtn);
            group.appendChild(labelRow);
            group.appendChild(desc);
        } else {
            group.appendChild(labelRow);
        }

        const input = document.createElement('input');
        input.type = 'range';
        input.min = param.min;
        input.max = param.max;
        input.step = param.step;
        input.value = param.default;
        input.name = param.name;

        const valueSpan = label.querySelector('.value');
        input.addEventListener('input', () => {
            valueSpan.textContent = input.value;
            if (onChange) onChange();
        });

        group.appendChild(input);
        container.appendChild(group);

        inputs[param.name] = { input, valueSpan };
    });

    return {
        getValues() {
            const values = {};
            for (const [name, entry] of Object.entries(inputs)) {
                values[name] = parseFloat(entry.input.value);
            }
            return values;
        },
        setValues(obj) {
            for (const [name, value] of Object.entries(obj)) {
                if (inputs[name]) {
                    inputs[name].input.value = value;
                    inputs[name].valueSpan.textContent = value;
                }
            }
        }
    };
}
