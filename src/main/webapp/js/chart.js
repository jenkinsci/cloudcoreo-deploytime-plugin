
function generateChartFromData(data) {
    const chartData = [];

    data.sort((first, second) => {
        const f = parseInt(first.build);
        const s = parseInt(second.build);
        if (f < s) return -1;
        if (f > s) return 1;
        return 0;
    });
    for (let i = 0; i < data.length; i++) {
        const result = data[i];
        const violations = result.violations;
        let low = 0;
        let medium = 0;
        let high = 0;
        if (Object.keys(violations).length > 0) {
            if (violations.LOW && violations.LOW.size() > 0) {
                low = violations.LOW.size();
            }
            if (violations.MEDIUM && violations.MEDIUM.size() > 0) {
                medium = violations.MEDIUM.size();
            }
            if (violations.HIGH && violations.HIGH.size() > 0) {
                high = violations.HIGH.size();
            }
        }
        chartData.push({
            build: result.build,
            low: low,
            medium: medium,
            high: high
        });
    }
    return chartData;
}

function initialize(data) {

    function zoomChart(){
        chart.zoomToIndexes(chart.dataProvider.length - 20, chart.dataProvider.length - 1);
    }

    const chartData = generateChartFromData(data);

    const chart = AmCharts.makeChart("chart", {
        "type": "serial",
        "legend": {
            "useGraphSettings": true
        },
        "dataProvider": chartData,
        "synchronizeGrid": true,
        "valueAxes": [{
            "id": "v1",
            "axisColor": "#0e9a75",
            "axisThickness": 2,
            "axisAlpha": 1,
            "position": "left"
        }],
        "graphs": [{
            "valueAxis": "v1",
            "lineColor": "#9fca4d",
            "bullet": "triangleUp",
            "bulletBorderThickness": 1,
            "hideBulletsCount": 30,
            "title": "Low",
            "valueField": "low",
            "fillAlphas": 0
        }, {
            "valueAxis": "v2",
            "lineColor": "#fcbc51",
            "bullet": "square",
            "bulletBorderThickness": 1,
            "hideBulletsCount": 30,
            "title": "Medium",
            "valueField": "medium",
            "fillAlphas": 0
        }, {
            "valueAxis": "v3",
            "lineColor": "#de583c",
            "bullet": "round",
            "bulletBorderThickness": 1,
            "hideBulletsCount": 30,
            "title": "High",
            "valueField": "high",
            "fillAlphas": 0
        }],
        "chartScrollbar": {},
        "chartCursor": {
            "cursorPosition": "mouse"
        },
        "categoryField": "build",
        "categoryAxis": {
            "axisColor": "#0e9a75",
            "minorGridEnabled": true
        },
        // "export": {
        //     "enabled": true,
        //     "position": "bottom-right"
        // }
    });

    chart.addListener("dataUpdated", zoomChart);
    zoomChart();
}