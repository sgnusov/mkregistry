const Modal = {
	template: `
		<div v-show="shown" class="modal">
			<div class="content">
				<div class="query">{{query}}</div>
				<template v-if="mode === 'prompt'">
					<input ref="input" class="input" @keypress.enter="promptFinish" v-model="promptText"><!--
					--><button class="button" @click="promptFinish">&gt;</button>
				</template>
				<template v-else-if="mode === 'select'">
					<button
						v-for="button in buttons"
						class="button"
						@click="selectFinish(button)"
						ref="button"
					>
						{{button}}
					</button>
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
			promptCb: null,
			promptText: "",
			buttons: []
		};
	},
	methods: {
		_show() {
			this.shown = true;
			document.addEventListener("keydown", this.onKeyDown);
		},
		_hide() {
			this.shown = false;
			document.removeEventListener("keydown", this.onKeyDown);
		},

		prompt(query) {
			this.query = query;
			this.promptText = "";
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
			this.promptCb(this.promptText);
		},

		select(query, buttons) {
			this.query = query;
			this.buttons = buttons;
			this.mode = "select";
			this._show();
			setTimeout(() => {
				this.$refs.button[0].focus();
			}, 0);
			return new Promise(resolve => {
				this.promptCb = resolve;
			});
		},
		selectFinish(value) {
			this._hide();
			this.promptCb(value);
		},

		alert(query) {
			this.query = query;
			this.mode = "alert";
			this._show();
			setTimeout(() => {
				this.$refs.button.focus();
			}, 0);
		},

		onKeyDown(e) {
			if(e.key === "Escape") {
				this._hide();
				if(this.mode === "prompt" || this.mode === "select") {
					this.promptCb(null);
				}
			}
		}
	}
};