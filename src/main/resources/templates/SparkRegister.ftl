<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8">
        <meta http-equiv="x-ua-compatible" content="ie=edge">
        <title>Register</title>
    </head>

    <body>
        <h1>Register</h1>
        <form action="/register" method="POST">
            <input type="hidden" name="nonce" id="nonce">
            Login: <input type="text" name="login" id="login"><br>
            Password: <input type="text" name="password" id="password"><br>
            PoW: <input type="button" id="nonceButton" onclick="generateNonce()" value="Generate nonce (this might take some time)"><br>
            <input type="submit" value="Register" onclick="register()"> ${error}
        </form>
        <a href="/">Login</a>

        <script type="text/javascript">
            let nonceLogin = null;

            async function generateNonce() {
                login.disabled = true;
                password.disabled = true;
                document.querySelector("#nonceButton").disabled = true;

                const pref = new Uint8Array(await crypto.subtle.digest("SHA-256", new Uint8Array(login.value.split("").map(c => c.charCodeAt(0)))));

                let nonce = 0;
                while(true) {
                    let buffer = new Uint8Array(36);
                    for(let i = 0; i < 32; i++) {
                        buffer[i] = pref[i];
                    }
                    buffer[32] = (nonce >> 0) & 0xFF;
                    buffer[33] = (nonce >> 8) & 0xFF;
                    buffer[34] = (nonce >> 16) & 0xFF;
                    buffer[35] = (nonce >> 24) & 0xFF;
                    const sha = new Uint8Array(await crypto.subtle.digest("SHA-256", buffer));
                    if(sha[0] === 0 && sha[1] <= 1) {
                        break;
                    }
                    nonce++;
                }

                nonceLogin = login.value;
                login.disabled = false;
                password.disabled = false;
                document.querySelector("#nonceButton").disabled = false;
                document.querySelector("#nonceButton").value = ("0000000" + nonce.toString(16)).slice(-8);
                document.querySelector("#nonce").value = ("0000000" + nonce.toString(16)).slice(-8);
            }

            function register() {
                if(login.value !== nonceLogin) {
                    generateNonce();
                    return false;
                }
            }
        </script>
    </body>
</html>