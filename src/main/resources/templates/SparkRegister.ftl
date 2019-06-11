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
            Login: <input type="text" name="login"><br>
            Password: <input type="text" name="password"><br>
            <input type="submit" value="Register"> ${error}
        </form>
        <a href="/">Login</a>
    </body>
</html>