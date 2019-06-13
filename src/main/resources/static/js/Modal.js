const Modal = {
	template: `
		<div v-show="shown" class="modal">
			<div class="content">
				<div class="query">{{query}}</div>
				<template v-if="mode === 'prompt'">
					<input ref="input" class="input" @keypress.enter="promptFinish">
					<button class="button" @click="promptFinish">&gt;</button>
				</template>
				<template v-else-if="mode === 'alert'">
					<button ref="button" class="button" @click="_hide">Ok</button>
				</template>
			</div>
		</div>
	`,
	data() {
		return {
			shown: false,
			query: "",
			mode: "",
			promptCb: null
		};
	},
	methods: {
		_show() {
			this.shown = true;
		},
		_hide() {
			this.shown = false;
		},

		async prompt(query) {
			this.query = query;
			this.mode = "prompt";
			this._show();
			setTimeout(() => {
				this.$refs.input.focus();
			}, 0);
			return new Promise(resolve => {
				this.promptCb = resolve;
			});
		},
		promptFinish() {
			this._hide();
			this.promptCb();
		},

		alert(query) {
			this.query = query;
			this.mode = "alert";
			this._show();
			setTimeout(() => {
				this.$refs.button.focus();
			}, 0);
		}
	}
};