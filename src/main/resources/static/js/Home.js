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

            <div v-else-if="state === 'requests' || state === 'exchangeSuggestion'">
                <form action="/logout" method="POST" class="logout">
                    <button class="button">Logout</button>
                </form>
                <button class="button requests" @click="showHome()">
                    Back to my bears
                </button>

                <div :class="['bump', {big: state === 'requests', small: state === 'exchangeSuggestion'}]"></div>
                <keep-alive>
                    <BearList
                        :bears="requests"
                        :showInfo="state !== 'exchangeSuggestion'"
                        :actions="requestsActions"
                        :inactiveActions="requestsActions"
                        @accept="startExchangeSuggestion"
                        ref="requestsList"
                    />
                </keep-alive>
                <div :class="{away: state !== 'exchangeSuggestion'}" style="transition: all 0.2s">
                    <keep-alive>
                        <div class="hr"></div>
                        <BearList
                            :bears="bears"
                            :showInfo="true"
                            :actions="exchangeSuggestionActions"
                            :inactiveActions="inactiveExchangeSuggestionActions"
                            @cancel="stopExchangeSuggestion"
                            @cancelExchange="cancelExchange"
                            @suggest="doExchangeSuggest"
                        />
                    </keep-alive>
                </div>
            </div>

            <div v-else-if="state === 'suggestions'">
                <form action="/logout" method="POST" class="logout">
                    <button class="button">Logout</button>
                </form>
                <button class="button requests" @click="showHome()">
                    Back to my bears
                </button>

                <div class="bump big"></div>
                <div class="suggestion">
                    <Bear
                        v-bind="suggestions[0]"
                        :isBig="true"
                        :isTiny="false"
                        :showInfo="true"
                        :actions="suggestionsActionsOur"
                        @reject="rejectExchange"
                    />
                    <Bear
                        v-bind="suggestions[0].suggestedBear"
                        :isBig="true"
                        :isTiny="false"
                        :showInfo="true"
                        :actions="suggestionsActionsTheir"
                        @accept="acceptExchange"
                    />
                </div>
            </div>

            <div v-else>
                <form action="/logout" method="POST" class="logout">
                    <button class="button">Logout</button>
                </form>
                <button class="button requests" v-if="requests.length > 0" @click="showExchangeRequests()">
                    <template v-if="requests.length === 1">One exchange request</template>
                    <template v-else>{{requests.length}} exchange requests</template>
                </button>
                <button class="button suggestions" v-if="suggestions.length > 0" @click="showExchangeSuggestions()">
                    <template v-if="suggestions.length === 1">One exchange to be finished</template>
                    <template v-else>{{suggestions.length}} exchanges to be finished</template>
                </button>

                <div :class="['bump', {big: state === 'home', small: state === 'mixing'}]"></div>
                <BearList
                    :bears="bears"
                    :showInfo="state === 'home'"
                    :actions="homeActions"
                    :inactiveActions="inactiveHomeActions"
                    @mix="startMix"
                    @present="startPresent"
                    @exchange="startExchange"
                    @cancelExchange="cancelExchange"
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
                        @cancelExchange="cancelExchange"
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
            requests: [],
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
            requestsActions: [
                {text: "Accept request", name: "accept"}
            ],
            exchangeDoneActions: [
                {text: "Yay!", name: "close"}
            ],
            inactiveExchangeSuggestionActions: [
                {text: "Cancel exchange with another friend", name: "cancelExchange"}
            ],
            exchangeSuggestionActions: [
                {text: "Suggest", name: "suggest"},
                {text: "Cancel", name: "cancel"}
            ],
            suggestionsActionsOur: [
                {text: "Reject: Leave this bear", name: "reject"}
            ],
            suggestionsActionsTheir: [
                {text: "Accept: Exchange with this bear", name: "accept"}
            ]
        };
    },
    mounted() {
        this.loadInterval();
    },
    methods: {
        async loadInterval() {
            await this.loadBears();
            await this.loadRequests();
            setTimeout(() => this.loadInterval(), 5000);
        },
        async loadBears() {
            this.bears = await API.getBears();
        },
        async loadRequests() {
            this.requests = await API.getRequests();
        },

        startMix() {
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
        async cancelExchange(bear) {
            this.state = "exchangeCancelInProgress";
            try {
                if(bear.init) {
                    await API.cancelExchange(bear);
                } else if(bear.suggest) {
                    await API.cancelExchangeSuggest(bear, bear.suggest);
                }
                await this.loadBears();
                this.state = "home";
            } catch(e) {
                this.error = e.toString();
                this.state = "error";
            }
        },

        async startExchangeSuggestion() {
            this.state = "exchangeSuggestion";
        },
        stopExchangeSuggestion() {
            this.state = "requests";
        },
        async doExchangeSuggest(bear) {
            const friendBear = this.bears[this.$refs.requestsList.currentBear];
            this.state = "exchangeInProgress";
            try {
                await API.suggestExchange(bear, friendBear);
                this.state = "home";
            } catch(e) {
                this.error = e.toString();
                this.state = "error";
            }
        },

        async acceptExchange() {
            this.state = "exchangeInProgress";
            try {
                await API.acceptExchange(
                    this.suggestions[0],
                    this.suggestions[0].suggestedBear,
                    this.suggestions[0].suggestedBear.ownerLogin
                );
                this.state = "home";
            } catch(e) {
                this.error = e.toString();
                this.state = "error";
            }
        },
        async rejectExchange() {
            this.state = "exchangeCancelInProgress";
            try {
                await API.rejectExchange(
                    this.suggestions[0],
                    this.suggestions[0].suggestedBear,
                    this.suggestions[0].suggestedBear.ownerLogin
                );
                this.state = "home";
            } catch(e) {
                this.error = e.toString();
                this.state = "error";
            }
        },

        showExchangeRequests() {
            this.state = "requests";
        },
        showExchangeSuggestions() {
            this.state = "suggestions";
        },
        showHome() {
            this.state = "home";
        },
        errorOk() {
            this.state = "home";
        }
    },
    computed: {
        suggestions() {
            return this.bears.filter(bear => !bear.active && bear.init && bear.suggestedBear);
        }
    }
};