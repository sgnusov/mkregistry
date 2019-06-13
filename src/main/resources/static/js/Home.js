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

            <div class="status" v-else-if="state === 'exchangeInProgress'">
                <div class="big">Exchanging...</div>
            </div>
            <div class="status" v-else-if="state === 'exchangeDone'">
                <div class="big">You've got a new bear!</div>
                <Bear
                    v-bind="bear"
                    :isBig="true"
                    :isTiny="false"
                    :showInfo="true"
                    :actions="exchangeDoneActions"
                    @close="doneExchange"
                />
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
                    @exchange="startExchange"
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
                {text: "Present", name: "present"},
                {text: "Exchange", name: "exchange"}
            ],
            mixingActions: [
                {text: "Mix", name: "mix"},
                {text: "Cancel", name: "cancel"}
            ],
            mixDoneActions: [
                {text: "Yay!", name: "close"}
            ],
            exchangeDoneActions: [
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
            if(!login) {
                return;
            }
            this.state = "presentInProgress";
            try {
                await API.present(bear, login);
                await this.loadBears();
                this.$refs.modal.alert("Present sent!");
                this.state = "home";
            } catch(e) {
                this.error = e.toString();
                this.state = "error";
            }
        },

        async startExchange(bear) {
            const query = await this.$refs.modal.select("Has your friend given your a key?", ["Yes", "No"]);
            if(!query) {
                return;
            }
            if(query === "Yes") {
                const key = await this.$refs.modal.prompt("What was the key?");
                if(!key) {
                    return;
                }
                const login = await this.$refs.modal.prompt("What's your friend's login?");
                if(!login) {
                    return;
                }
                this.state = "exchangeInProgress";
                try {
                    this.bear = await API.exchange(bear, login, key);
                    await this.loadBears();
                    this.state = "exchangeDone";
                } catch(e) {
                    this.error = e.toString();
                    this.state = "error";
                }
            } else {
                this.state = "exchangeInProgress";
                try {
                    const key = await API.changeKey(bear);
                    this.state = "home";
                    await this.$refs.modal.alert(`Share this key: ${key}`);
                } catch(e) {
                    this.error = e.toString();
                    this.state = "error";
                }
            }
        },
        doneExchange() {
            this.state = "home";
        },

        errorOk() {
            this.state = "home";
        }
    }
};