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

        <button onclick="mix()">Mix</button>

        <form action="/logout" method="POST">
            <button>Logout</button>
        </form>

        <form action="/api/mix" method="POST" id="mixform">
            <input type="hidden" name="color1" id="color1">
            <input type="hidden" name="color2" id="color2">
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
                document.querySelector("#color1").value = colors[0];
                document.querySelector("#color2").value = colors[1];
                document.querySelector("#mixform").submit();
            }
        </script>
    </body>
</html>