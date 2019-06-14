<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8">
        <meta http-equiv="x-ua-compatible" content="ie=edge">
        <title>Welcome</title>
    </head>

    <body>
        <h1>Hello, ${login}!</h1>
        <#if error != "">
            <div style="color: #ff0000; font-size: 1.5em">${error}</div>
        </#if>
        <#if bears?has_content>
            <h2>Your bears:</h2>
        </#if>
        <ul>
            <#list bears as bear>
                <li>
                    Color: ${bear.color}
                    <!--
                    <form action="/api/swap/initialize" method="POST">
                        <input type="hidden" name="color" value="${bear.color}">
                        <input type="text" name="login" placeholder="Login">
                        <button>Swap with friend</button>
                    </form>
                    -->
                    <form action="/api/swap/initialize" method="POST">
                        <input type="hidden" name="color" value="${bear.color}">
                        <input type="text" name="friend" placeholder="Login">
                        <button>Swap with friend</button>
                    </form>
                    <form action="/api/present" method="POST">
                        <input type="hidden" name="color" value="${bear.color}">
                        <input type="checkbox" data-color="${bear.color}" class="mixcheckbox">
                        <input type="text" name="receiver" placeholder="Your friend's login">
                        <button>Present</button>
                    </form>
                </li>
            <#else>
                <h2>You do not have bears</h2>
            </#list>
        </ul>

        <ul>
            <#list requests as request>
                <li>
                    Color: ${request.issuerBearColor} </br>
                    Login: ${request.issuerLogin}
                    <form action="/api/swap/suggest" method="POST" id="suggestform">
                        <input type="hidden" name="color2" class="color2">
                        <input type="hidden" name="color1" value="${request.issuerBearColor}">
                        <input type="hidden" name="friend" value="${request.issuerLogin}">
                    </form>
                </li>
            </#list>
        </ul>

        <ul>
            <#list queries as query>
                <li>
                    Your bear: ${query.issuerBearColor} </br>
                    Your friend's bear: ${query.receiverBearColor} </br>
                    Friend: ${query.receiverLogin}
                    <form action="/api/swap/accept" method="POST" id="suggestform">
                        <input type="hidden" name="color2" value="${query.receiverBearColor}">
                        <input type="hidden" name="color1" value="${query.issuerBearColor}">
                        <input type="hidden" name="friend" value="${query.receiverLogin}">
                        <button>Accept</button>
                    </form>
                </li>
            </#list>
        </ul>

        <button onclick="mix()">Mix</button>
        <button onclick="suggest()">Suggest</button>

        <form action="/logout" method="POST">
            <button>Logout</button>
        </form>

        <form action="/api/mix" method="POST" id="mixform">
            <input type="hidden" name="color1" class="color1">
            <input type="hidden" name="color2" class="color2">
        </form>

        <script type="text/javascript">
            function mix() {
                const colors = Array.from(document.querySelectorAll(".mixcheckbox"))
                    .filter(checkbox => checkbox.checked)
                    .map(checkbox => checkbox.dataset.color);
                if(colors.length !== 2) {
                    alert("2 bears have to be selected");
                    return;
                }
                document.querySelector(".color1").value = colors[0];
                document.querySelector(".color2").value = colors[1];
                document.querySelector("#mixform").submit();
            }
            function suggest() {
                const colors = Array.from(document.querySelectorAll(".mixcheckbox"))
                    .filter(checkbox => checkbox.checked)
                    .map(checkbox => checkbox.dataset.color);
                if(colors.length !== 1) {
                    alert("1 bears have to be selected");
                    return;
                }
                document.querySelector(".color2").value = colors[0];
                document.querySelector("#suggestform").submit();
            }
        </script>
    </body>
</html>