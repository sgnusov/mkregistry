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
            <div class="status" v-else-if="state === 'exchangeCancelInProgress'">
                <div class="big">Canceling exchange...</div>
            </div>

            <div class="status" v-else-if="state === 'error'">
                <div class="big">{{error}}</div>
                <button class="button" @click="errorOk">Ok</button>
            </div>

            <div v-else>
                <form action="/logout" method="POST" class="logout">
                    <button class="button">Logout</button>
                </form>

                <div :class="['bump', {big: state === 'home', small: state === 'mixing'}]"></div>
                <BearList
                    :bears="bears"
                    :showInfo="state === 'home'"
                    :actions="homeActions"
                    :inactiveActions="inactiveHomeActions"
                    @mix="startMix"
                    @present="startPresent"
                    @exchange="startExchange"
                    @cancelExchange="cancelExchangeInit"
                    ref="defaultBearList"
                />
                <div :class="{away: state !== 'mixing'}" style="transition: all 0.2s">
                    <div class="hr"></div>
                    <BearList
                        :bears="bears"
                        :showInfo="true"
                        :actions="mixingActions"
                        :inactiveActions="inactiveHomeActions"
                        @cancel="cancelMix"
                        @cancelExchange="cancelExchangeInit"
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
                color: null,
                hair: null,
                lips: null
            },
            error: "",
            homeActions: [
                {text: "Mix", name: "mix"},
                {text: "Present", name: "present"},
                {text: "Exchange", name: "exchange"}
            ],
            inactiveHomeActions: [
                {text: "Cancel exchange", name: "cancelExchange"}
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
            const otherBear = this.bears[this.$refs.defaultBearList.currentBear];
            if(otherBear === bear) {
                this.$refs.modal.alert("You can't mix the same bears.");
                return;
            }
            this.state = "mixInProgress";
            try {
                this.bear = await API.mix(otherBear, bear);
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
            const login = await this.$refs.modal.prompt("What's your friend's login?");
            if(!login) {
                return;
            }
            this.state = "exchangeInProgress";
            try {
                await API.initExchange(bear, login);
                await this.loadBears();
                this.state = "home";
            } catch(e) {
                this.error = e.toString();
                this.state = "error";
            }
        },

        async cancelExchangeInit(bear) {
            this.state = "exchangeCancelInProgress";
            try {
                await API.cancelExchange(bear);
                await this.loadBears();
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