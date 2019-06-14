<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8">
        <title>Register</title>
        <link rel="stylesheet" type="text/css" href="css/common.css">
        <link rel="stylesheet" type="text/css" href="css/login.css">
    </head>

    <body>
        <h1>Register</h1>

        <form action="/register" method="POST" onsubmit="return onSubmit(event)">
            <input type="hidden" name="nonce" id="nonce">
            <table>
                <tbody>
                    <tr>
                        <th>Login:</th>
                        <th><input type="text" class="input" id="login" name="login"></th>
                    </tr>
                    <tr>
                        <th>Password:</th>
                        <th><input type="text" class="input" name="password"></th>
                    </tr>
                    <tr>
                        <th>Party:</th>
                        <th>
                            <select class="input" name="party">
                                <option value="127.0.0.1:10009">Microsoft &copy;</option>
                                <option value="127.0.0.1:10013">Apple &copy;</option>
                            </select>
                        </th>
                    </tr>
                </tbody>
            </table>
            <input type="submit" class="button" value="Register"><br>
            <div class="error" id="error">${error}</div>
        </form>
        <a class="another-link" href="/">Have an account already? Login</a>

        <script type="text/javascript" src="js/sha256.min.js"></script>
        <script type="text/javascript">
            let isDoingPoW = false, isPoWDone = false;

            function getSha256(buf) {
                const hex = sha256(buf);
                const res = new Uint8Array(hex.length / 2);
                for(let i = 0; i < res.length; i++) {
                    res[i] = (parseInt(hex[i * 2], 16) << 4) | parseInt(hex[i * 2 + 1], 16);
                }
                return new Promise(resolve => setTimeout(() => resolve(res), 0));
            }

            function onSubmit(e) {
                if(isPoWDone) {
                    return;
                }
                e.preventDefault();
                if(isDoingPoW) {
                    return;
                }
                document.querySelector("#error").innerHTML = "We require a little PoW to register. Please wait...";
                isDoingPoW = true;

                const login = document.querySelector("#login").value;
                document.querySelector("#login").disabled = true;

                (async () => {
                    const pref = await getSha256(new Uint8Array(login.split("").map(c => c.charCodeAt(0))));
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
                        const sha = await getSha256(buffer);
                        if(sha[0] === 0) {
                            break;
                        }
                        nonce++;
                    }

                    document.querySelector("#login").disabled = false;
                    document.querySelector("#nonce").value = nonce.toString(16);
                    isPoWDone = true;
                    e.target.submit();
                })();
            }
        </script>
    </body>
</html>