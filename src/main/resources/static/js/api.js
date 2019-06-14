const IS_LOCAL = location.protocol === "file:";


async function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

const API = new class API {
    async call(action, method="POST", args={}) {
        const result = await fetch(`/api/${action}`, {
            method,
            body: method === "POST"
                ? Object.keys(args).map(key => `${key}=${args[key]}`).join("&")
                : null,
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            }
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
                {color: 12, lips: 12, hair: 12, active: true},
                {color: 78, lips: 156, hair: 156, active: false},
                {color: 153, lips: 17, hair: 17, active: true},
                {color: 211, lips: 200, hair: 200, active: false},
                {color: 10, lips: 13, hair: 13, active: true},
                {color: 100, lips: 100, hair: 100, active: true}
            ];
        }
        return (await this.callArray("bears", "GET"))
            .map(bear => ({
                color: parseInt(bear.color),
                lips: parseInt(bear.lips),
                hair: parseInt(bear.hair)
            }));
    }
    async mix(bear1, bear2) {
        if(IS_LOCAL) {
            await sleep(1000);
            return {
                color: Math.floor((bear1.color + bear2.color) / 2),
                lips: Math.floor((bear1.lips + bear2.lips) / 2),
                hair: Math.floor((bear1.hair + bear2.hair) / 2)
            };
        }
        const bear = await this.callMap("mix", "POST", {
            color1: bear1.color,
            lips1: bear1.lips,
            hair1: bear1.hair,
            color2: bear2.color,
            lips2: bear2.lips,
            hair2: bear2.hair
        });
        if(bear.error) {
            throw new Error(bear.error);
        }
        return {
            color: parseInt(bear.color),
            lips: parseInt(bear.lips),
            hair: parseInt(bear.hair)
        };
    }
    async present(bear, login) {
        if(IS_LOCAL) {
            await sleep(1000);
            return true;
        }
        const res = await this.call("present", "POST", {
            receiver: login,
            color: bear.color,
            lips: bear.lips,
            hair: bear.hair
        });
        if(res !== "") {
            throw new Error(res);
        }
        return true;
    }

    async initExchange(bear, login) {
        if(IS_LOCAL) {
            await sleep(1000);
            return true;
        }
        const res = await this.call("swap/init", "POST", {
            color: bear.color,
            lips: bear.lips,
            hair: bear.hair,
            login
        });
        if(res !== "") {
            throw new Error(res);
        }
        return true;
    }
    async cancelExchange(bear) {
        if(IS_LOCAL) {
            await sleep(1000);
            return true;
        }
        const res = await this.call("swap/init/cancel", "POST", bear);
        if(res !== "") {
            throw new Error(res);
        }
        return true;
    }
    async suggestExchange(bear, friendBear, friendLogin) {
        if(IS_LOCAL) {
            await sleep(1000);
            return true;
        }
        const res = await this.call("swap/suggest", "POST", {
            color1: bear.color,
            lips1: bear.lips,
            hair1: bear.hair,
            color2: friendBear.color,
            lips2: friendBear.lips,
            hair2: friendBear.hair,
            friendLogin
        });
        if(res !== "") {
            throw new Error(res);
        }
        return true;
    }
    async acceptExchangeSuggestion(bear, friendBear, friendLogin) {
        if(IS_LOCAL) {
            await sleep(1000);
            return true;
        }
        const res = await this.callMap("swap/accept", "POST", {
            color1: bear.color,
            lips1: bear.lips,
            hair1: bear.hair,
            color2: friendBear.color,
            lips2: friendBear.lips,
            hair2: friendBear.hair,
            friendLogin
        });
        if(res !== "") {
            throw new Error(res);
        }
        return true;
    }
};