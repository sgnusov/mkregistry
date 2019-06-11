<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8">
        <meta http-equiv="x-ua-compatible" content="ie=edge">
        <title>Welcome</title>
    </head>

    <body>
        <h1>Hello, ${login}!</h1>
        <form action="/logout" method="POST">
            <button>Logout</button>
        </form>
    </body>
</html>