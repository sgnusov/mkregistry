<html>

<head>
    <meta charset="utf-8">
    <meta http-equiv="x-ua-compatible" content="ie=edge">
    <title>Uploader corda</title>
    <link href="https://use.fontawesome.com/releases/v5.0.6/css/all.css" rel="stylesheet">
    <link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css" integrity="sha384-MCw98/SFnGE8fJT3GXwEOngsV7Zt27NXFoaoApmYm81iuXoPkFOJwJ8ERdknLPMO" crossorigin="anonymous">
    <link rel="stylesheet" id="main-stylesheet" data-version="1.1.0" href="styles/shards-dashboards.1.1.0.min.css">
    <link rel="stylesheet" href="styles/extras.1.1.0.min.css">
    <script async="" defer="" src="https://buttons.github.io/buttons.js"></script>
</head>

<body class="h-100">

<#if registered == false>
  <div class="py-5">
     <div class="container">
       <div class="row">
        <div class="d-inline-flex align-items-center justify-content-center flex-row w-100">
               <button id=('id02') onclick="
               document.getElementById('id01').style.display='block';
                 document.getElementById('id02').style.display='none';"

               " class="btn btn-outline-danger btn-lg w-100">Вход</button>
        </div>
     </div>
   </div>
   <div>
     <!-- Button to open the modal login form -->
     <!-- The Modal -->
     <div id="id01" class="modal" style="display: none;">
       <span
       onclick="
       document.getElementById('id01').style.display='none';
       document.getElementById('id02').style.display='block';"
        class="close" title="Close Modal">x</span>
       <!-- Modal Content -->
       <form class="modal-content animate" method="post" action="/login">
         <div class="container d-inline-flex">
           <label for="uname"></label>
           <input placeholder="Enter Username" name="uname" required="" class="form-control is-invalid" type="text">
           <label for="psw"></label>
           <input placeholder="Enter Password" name="upass" required="" class="form-control is-invalid" type="password">
         </div>
         <div style="" class="justify-content-center d-inline-flex border-bottom w-100 text-center mt-2">
           <button type="submit" class="mb-2 btn btn-outline-danger mr-2 btn-lg">Войти</button>
           <button type="button" onclick="document.getElementById('id01').style.display='none'" class="mb-2 btn btn-outline-danger mr-2 btn-lg">Отмена</button>
         </div>
       </form>
     </div>
   </div>
    <#else>

        <label class="align-items-center justify-content-center d-flex">Привет, ${username}!</label>
        <form action="/logout" method="post" class="align-items-center justify-content-center d-flex">
           <button type="submit" class="mb-2 btn btn-outline-danger mr-2 btn-lg">Выйти</button>
        </form>
    <#if right == true>
        <div class="my-4 mx-auto border-left">
            <main class="main-content ">
                <div class="main-content-container container-fluid text-center">
                    <div class="page-header row no-gutters py-4 mb-3 text-center">
                        <div class="col-12 text-center">
                            <h5 class="page-title align-items-center">Загрузка</h5>
                        </div>
                    </div>
                    <div class="row">
                        <div class="col-lg-12 mb-4">
                            <div class="card card-small mb-4">
                                <ul class="list-group list-group-flush">
                                    <li class="list-group-item p-3">
                                        <strong class="text-muted d-block mb-2 text-center">Выберите файл</strong>
                                        <label class="text-muted d-block mb-2 text-center">${status}</label>
                                        <div class="row">
                                            <div class="col">
                                                <form action="/" method='post' enctype='multipart/form-data'>
                                                    <label for="upload">
                                                        <span class="mb-2 btn btn-outline-danger mr-2 btn-block btn-lg" aria-hidden="true">Выбор</span>
                                                        <input type="file" id=upload name="uploaded_file" style="display:none">
                                                    </label>
                                                    <button class="mb-2 btn btn-outline-danger mr-2 btn-lg" style="">Загрузить</button>
                                                </form>
                                            </div>
                                        </div>
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    <#else>
        <div class="my-4 mx-auto border-left">
            <main class="main-content ">
                <div class="main-content-container container-fluid text-center">
                    <div class="page-header row no-gutters py-4 mb-3 text-center">
                        <div class="col-12 text-center">
                            <h5 class="page-title align-items-center">Вам не разрешено загружать файлы</h5>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    </#if>
</#if>
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