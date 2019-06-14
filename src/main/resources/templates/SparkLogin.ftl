<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8">
        <title>Login</title>
        <link rel="stylesheet" type="text/css" href="css/common.css">
        <link rel="stylesheet" type="text/css" href="css/login.css">
    </head>

    <body>
        <h1>Login</h1>

        <form action="/login" method="POST">
            <table>
                <tbody>
                    <tr>
                        <th>Login:</th>
                        <th><input type="text" class="input" name="login"></th>
                    </tr>
                    <tr>
                        <th>Password:</th>
                        <th><input type="text" class="input" name="password"></th>
                    </tr>
                </tbody>
            </table>
            <input type="submit" class="button" value="Login"><br>
            <div class="error">${error}</div>
        </form>
        <a class="another-link" href="/register">Don't have an account? Register</a>
    </body>
</html>