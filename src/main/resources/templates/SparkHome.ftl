<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8">
        <meta http-equiv="x-ua-compatible" content="ie=edge">
        <title>Welcome</title>
    </head>

    <body>
        <h1>Hello, ${login}!</h1>

        <h2>Your bears:</h2>
        <ul>
            <#list bears as bear>
                <li>
                    Color: ${bear.color}
                    <form action="/api/present" method="POST">
                        <input type="hidden" name="color" value="${bear.color}">
                        <input type="text" name="receiver" placeholder="Your friend's login">
                        <button>Present</button>
                    </form>
                </li>
            </#list>
        </ul>

        <form action="/logout" method="POST">
            <button>Logout</button>
        </form>
    </body>
</html>