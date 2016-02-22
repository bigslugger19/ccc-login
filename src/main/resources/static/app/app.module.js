(function () {
    angular.module('CCC', [
        'ngResource',
        'ngSanitize',
        'ui.router',
        'ui.bootstrap',
        'datatables',
        'xeditable',
        'mwl.confirm',
        'msl.uploads',
        'toggle-switch'
    ])
        .config(config)
        .run(run);

    config.$inject = [];

    function config() {
    	// TODO: config stuff
    	console.log('in config');
    }

    run.$inject = ['$rootScope', '$log'];

    function run($rootScope, $log) {
    	$log.debug('in run');
    }
}());