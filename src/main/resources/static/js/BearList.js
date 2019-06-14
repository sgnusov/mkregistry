const BearList = {
    template: `
        <div :class="['bear-list', {big: showInfo}]">
            <template v-if="bears.length > 0">
                <button
                    @click="prev"
                    class="navigate"
                    :style="{visibility: currentBear - 1 >= 0 ? 'visible' : 'hidden'}"
                >
                    &lt;
                </button>
                <div class="container">
                    <div class="inner-container">
                        <Bear
                            v-show="currentBear <= 1"
                            :color="0" :hair="0" :lips="0"
                            :isBig="false"
                            :isTiny="true"
                            :showInfo="false"
                            :actions="actions"
                        />
                        <Bear
                            v-show="currentBear === 0"
                            :color="0" :hair="0" :lips="0"
                            :isBig="false"
                            :isTiny="false"
                            :showInfo="false"
                            :actions="actions"
                            style="visibility: hidden; pointer-events: none"
                        />
                        <template v-for="bear, i in bears">
                            <Bear
                                :key="i"
                                v-show="Math.abs(i - currentBear) <= 2"
                                v-bind="bear"
                                :isBig="i === currentBear"
                                :isTiny="Math.abs(i - currentBear) === 2"
                                :showInfo="showInfo"
                                :actions="bear.active ? actions : inactiveActions"
                                @action="name => $emit(name, bear)"
                            />
                        </template>
                        <Bear
                            v-show="currentBear === bears.length - 1"
                            :color="0" :hair="0" :lips="0"
                            :isBig="false"
                            :isTiny="false"
                            :showInfo="false"
                            :actions="actions"
                            style="visibility: hidden; pointer-events: none"
                        />
                        <Bear
                            v-show="currentBear >= bears.length - 2"
                            :color="0" :hair="0" :lips="0"
                            :isBig="false"
                            :isTiny="true"
                            :showInfo="false"
                            :actions="actions"
                        />
                    </div>
                </div>
                <button
                    @click="next"
                    class="navigate"
                    :style="{visibility: currentBear + 1 < bears.length ? 'visible' : 'hidden'}"
                >
                    &gt;
                </button>
            </template>
            <template v-else>
                <h2>No bears here.</h2>
            </template>
        </div>
    `,
    props: {
        bears: Array,
        showInfo: Boolean,
        actions: Array,
        inactiveActions: Array
    },
    data() {
        return {
            bears: [],
            currentBear: 0,
            showInfo: true,
            actions: [],
            inactiveActions: []
        };
    },
    methods: {
        prev() {
            this.currentBear--;
        },
        next() {
            this.currentBear++;
        },
        goTo(i) {
            this.currentBear = i;
        }
    },
    computed: {
        selectedBear() {
            return this.bears[this.currentBear];
        }
    }
};