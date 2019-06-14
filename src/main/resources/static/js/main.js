Vue.component("Home", Home);
Vue.component("Bear", Bear);
Vue.component("BearList", BearList);
Vue.component("Modal", Modal);
new Vue({
    el: "#app",
    template: "<Home />"
});