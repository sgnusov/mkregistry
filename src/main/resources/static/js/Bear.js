const Bear = {
    template: `
        <div class="bear">
            <div :class="['icon', {'big': isBig, 'tiny': isTiny}]" :style="{backgroundColor: uiColor}"></div>
            <div class="info" v-if="isBig && showInfo">
                <h2>Characteristics</h2>
                <table>
                    <tbody>
                        <tr>
                            <th>Color</th>
                            <td>{{color}}</td>
                        </tr>
                    </tbody>
                </table>
                <button class="button" v-for="action in actions" @click="$emit('action', action.name); $emit(action.name)">{{action.text}}</button>
            </div>
        </div>
    `,
    props: {
        color: Number,
        isBig: Boolean,
        isTiny: Boolean,
        showInfo: Boolean,
        actions: Array
    },
    data() {
        return {
            color: 0,
            isBig: false,
            isTiny: false,
            showInfo: true,
            actions: []
        };
    },
    computed: {
        uiColor() {
            const color1 = "896248";
            const color2 = "ffb500";
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
                                ) * (this.color / 255)
                            ).toString(16)
                        ).slice(-2)
                    ))
                    .join("")
            );
        }
    }
};