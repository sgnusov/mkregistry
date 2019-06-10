<html>

<head>
    <meta charset="utf-8">
    <meta http-equiv="x-ua-compatible" content="ie=edge">
    <title>Info corda</title>
    <link href="https://use.fontawesome.com/releases/v5.0.6/css/all.css" rel="stylesheet">
    <link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css" integrity="sha384-MCw98/SFnGE8fJT3GXwEOngsV7Zt27NXFoaoApmYm81iuXoPkFOJwJ8ERdknLPMO" crossorigin="anonymous">
    <link rel="stylesheet" id="main-stylesheet" data-version="1.1.0" href="styles/shards-dashboards.1.1.0.min.css">
    <link rel="stylesheet" href="styles/extras.1.1.0.min.css">
    <script async="" defer="" src="https://buttons.github.io/buttons.js"></script>
</head>

<body class="h-100">
<div class="my-4 mx-auto border-left">
    <main class="main-content ">
        <div class="main-content-container container-fluid text-center">
            <div class="page-header row no-gutters py-4 mb-3 text-center">
                <div class="col-12 text-center">
                    <h5 class="page-title align-items-center">Файлы для оценки</h5>
                </div>
            </div>
            <div class="row">
                <div class="col-lg-12 mb-4">
                    <div class="card card-small mb-4">
                        <ul class="list-group list-group-flush">
                            <#list hashArray as x >
                                <li class="list-group-item p-3">
                                    <form method="post">
                                        <strong class="text-muted d-block mb-2 text-center">Файл</strong>
                                        <input name="hash" type="hidden" value=${x}>
                                        <div class="form-group">
                                            <label>${x}</label>
                                            <input name="mark" type="text" class="form-control" placeholder="Put your mark" aria-label="Put your mark" aria-describedby="basic-addon1">
                                        </div>
                                        <div class="row">
                                            <div class="col">
                                                <button type="submit" class="mb-2 btn btn-outline-danger mr-2 btn-lg" style="">Отправить</button>
                                            </div>
                                        </div>
                                    </form>
                                    <form method ='get'>
                                        <input name="hash" type="hidden" value=${x}>
                                        <button type='submit' class="btn btn-download mb-2 btn-outline-danger mr-2" id=${"d" + x}>Скачать файл</button>
                                    </form>
                                    <label class="text-muted d-block mb-2 text-center">${status}</label>
                                </li>
                            </#list>
                         </ul>
                    </div>
                </div>
            </div>
        </div>
    </main>
</div>
<script src="https://code.jquery.com/jquery-3.3.1.min.js" integrity="sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8=" crossorigin="anonymous"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.3/umd/popper.min.js" integrity="sha384-ZMP7rVo3mIykV+2+9J3UJ46jBk0WLaUAdn689aCwoqbBJiSnjAK/l8WvCWPIPm49" crossorigin="anonymous"></script>
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/js/bootstrap.min.js" integrity="sha384-ChfqqxuZUCnJSK3+MXmPNIyE6ZbWh2IMqE241rYiqJxyMiZ6OW/JmZQ5stwEULTy" crossorigin="anonymous"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.7.1/Chart.min.js"></script>
<script src="https://unpkg.com/shards-ui@latest/dist/js/shards.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/Sharrre/2.0.1/jquery.sharrre.min.js"></script>
<script src="scripts/extras.1.1.0.min.js"></script>
<script src="scripts/shards-dashboards.1.1.0.min.js"></script>
<script src="scripts/app/app-components-overview.1.1.0.js"></script>
</body>

</html>