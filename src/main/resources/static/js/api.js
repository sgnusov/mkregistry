const IS_LOCAL = location.protocol === "file:";


async function sleep(ms) {
	return new Promise(resolve => setTimeout(resolve, ms));
}

const API = new class API {
	async call(action, method="POST", args={}) {
		const result = await fetch(`/api/${action}`, {
			method,
			data: Object.keys(args).map(key => `${key}=${args[key]}`).join("&")
		});
		return result.text();
	}
	async callMap(action, method="POST", args={}) {
		return this.parseResultRow(await this.call(action, method, args));
	}
	async callArray(action, method="POST", args={}) {
		return (await this.call(action, method, args))
			.split("\n")
			.map(this.parseResultRow)
			.filter(row => row);
	}

	parseResultRow(row) {
		if(!row) {
			return null;
		}
		return row
			.split("&")
			.map(value => value ? value.split("=") : [null, null])
			.map(([key, value]) => (key ? {[key]: value} : {}))
			.reduce((a, b) => Object.assign(a, b), {});
	}


	async getUserData() {
		if(IS_LOCAL) {
			await sleep(500);
			return {
				login: "admin",
				partyAddress: "127.0.0.1:10009"
			};
		}
		return await this.callMap("user", "GET");
	}
	async getBears() {
		if(IS_LOCAL) {
			await sleep(500);
			return [
				{color: 12},
				{color: 78},
				{color: 153},
				{color: 211},
				{color: 10},
				{color: 100}
			];
		}
		return (await this.callArray("bears", "GET"))
			.map(bear => ({
				color: parseInt(bear.color)
			}));
	}
	async mix(bear1, bear2) {
		if(IS_LOCAL) {
			await sleep(1000);
			return {
				color: Math.floor((bear1.color + bear2.color) / 2)
			};
		}
		const bear = await this.callMap("mix", "POST", {
			color1: bear1.color,
			color2: bear2.color
		});
		if(bear.error) {
			throw new Error(bear.error);
		}
		return {
			color: parseInt(bear.ccolor)
		};
	}
	async present(bear, login) {
		if(IS_LOCAL) {
			await sleep(1000);
			return true;
		}
		const res = await this.call("present");
		if(res !== "") {
			throw new Error(res);
		}
		return true;
	}
};