const Home = {
    template: `
        <div style="height: 100%">
            <div class="status" v-if="state === 'mixInProgress'">
                <div class="big">Mixing...</div>
            </div>
            <div class="status" v-else-if="state === 'mixDone'">
                <div class="big">You've got a new bear!</div>
                <Bear
                    v-bind="bear"
                    :isBig="true"
                    :isTiny="false"
                    :showInfo="true"
                    :actions="mixDoneActions"
                    @close="cancelMix"
                />
            </div>

            <div class="status" v-else-if="state === 'presentInProgress'">
                <div class="big">Sending a present...</div>
            </div>

            <div class="status" v-else-if="state === 'error'">
                <div class="big">{{error}}</div>
                <button class="button" @click="errorOk">Ok</button>
            </div>

            <div v-else>
                <BearList
                    :bears="bears"
                    :showInfo="state === 'home'"
                    :actions="homeActions"
                    @mix="startMix"
                    @present="startPresent"
                    ref="defaultBearList"
                />
                <div :class="{away: state !== 'mixing'}" style="transition: all 0.2s">
                    <div class="hr"></div>
                    <BearList
                        :bears="bears"
                        :showInfo="true"
                        :actions="mixingActions"
                        @cancel="cancelMix"
                        @mix="doMix"
                    />
                </div>
            </div>

            <Modal ref="modal" />
        </div>
    `,
    data() {
        return {
            state: "home",
            bears: [],
            bear: {
                color: null
            },
            error: "",
            homeActions: [
                {text: "Mix", name: "mix"},
                {text: "Present", name: "present"}
            ],
            mixingActions: [
                {text: "Mix", name: "mix"},
                {text: "Cancel", name: "cancel"}
            ],
            mixDoneActions: [
                {text: "Yay!", name: "close"}
            ]
        };
    },
    mounted() {
        this.loadBears();
    },
    methods: {
        async loadBears() {
            this.bears = await API.getBears();
        },

        startMix(bear) {
            this.state = "mixing";
        },
        cancelMix() {
            this.state = "home";
        },
        async doMix(bear) {
            this.state = "mixInProgress";
            try {
                this.bear = await API.mix(this.bears[this.$refs.defaultBearList.currentBear], bear);
                await this.loadBears();
                this.state = "mixDone";
            } catch(e) {
                this.error = e.toString();
                this.state = "error";
            }
        },

        async startPresent(bear) {
            const login = await this.$refs.modal.prompt("What's your friend login?");
            this.state = "presentInProgress";
            try {
                await API.present(bear, login);
                this.$refs.modal.alert("Present sent!");
                this.state = "home";
            } catch(e) {
                this.error = e.toString();
                this.state = "error";
            }
        },

        errorOk() {
            this.state = "home";
        }
    }
};