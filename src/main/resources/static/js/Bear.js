const Bear = {
    template: `
        <div class="bear">
            <div :class="['icon', {'big': isBig, 'tiny': isTiny}]" :style="{backgroundColor: uiColor}">
                <div class="hair" :style="{backgroundColor: uiHair}"></div>
                <div class="hair" :style="{backgroundColor: uiHair}"></div>
                <div class="hair" :style="{backgroundColor: uiHair}"></div>
                <div class="hair" :style="{backgroundColor: uiHair}"></div>
                <div class="hair" :style="{backgroundColor: uiHair}"></div>
                <div class="hair" :style="{backgroundColor: uiHair}"></div>
                <br>
                <div class="eye eye1"></div>
                <div class="eye eye2"></div>
                <div class="nose"></div>
                <div class="lips"></div>
                <div class="mouth" :style="{backgroundColor: uiLips}"></div>
            </div>
            <div class="info" v-if="isBig && showInfo">
                <h2>Characteristics</h2>
                <table>
                    <tbody>
                        <tr>
                            <th>Color</th>
                            <td>{{color}}</td>
                        </tr>
                        <tr>
                            <th>Hair</th>
                            <td>{{hair}}</td>
                        </tr>
                        <tr>
                            <th>Lips</th>
                            <td>{{lips}}</td>
                        </tr>
                    </tbody>
                </table>
                <button class="button" v-for="action in actions" @click="$emit('action', action.name); $emit(action.name)">{{action.text}}</button>
            </div>
        </div>
    `,
    props: {
        color: Number,
        lips: Number,
        hair: Number,
        isBig: Boolean,
        isTiny: Boolean,
        showInfo: Boolean,
        actions: Array
    },
    data() {
        return {
            color: 0,
            lips: 0,
            hair: 0,
            isBig: false,
            isTiny: false,
            showInfo: true,
            actions: []
        };
    },
    methods: {
        colorBetween(color1, color2, value) {
            return (
                "#" +
                [0, 2, 4]
                    .map(i => (
                        (
                            "00" +
                            Math.floor(
                                parseInt(color1.substr(i, 2), 16) +
                                (
                                    parseInt(color2.substr(i, 2), 16) -
                                    parseInt(color1.substr(i, 2), 16)
                                ) * (value / 255)
                            ).toString(16)
                        ).slice(-2)
                    ))
                    .join("")
            );
        }
    },
    computed: {
        uiColor() {
            return this.colorBetween("896248", "ffb500", this.color);
        },
        uiLips() {
            return this.colorBetween("dd1353", "e40000", this.lips) + "7f";
        },
        uiHair() {
            return this.colorBetween("000000", "8c3419", this.hair) + "80";
        }
    }
};