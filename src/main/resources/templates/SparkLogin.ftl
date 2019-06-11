<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8">
        <meta http-equiv="x-ua-compatible" content="ie=edge">
        <title>Login</title>
    </head>

    <body>
        <h1>Login</h1>
        <form action="/login" method="POST">
            Login: <input type="text" name="login"><br>
            Password: <input type="text" name="password"><br>
            <input type="submit" value="Login"> ${error}
        </form>
        <a href="/register">Register</a>
    </body>
</html>