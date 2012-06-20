Event.observe(window, "load", function() {
	var jobNamesCheckboxes = $$('[name="jobNames"]');
	for(var i = 0; i < jobNamesCheckboxes.length; i++) {
		var jobCheckbox = jobNamesCheckboxes[i];
		jobCheckbox.observe('click', onJobNameCheckboxClick);
	}
});

function onJobNameCheckboxClick(event) {
	var selectedJobNames = $$('[name="jobNames"]');
	var selected = [];
	var index = 0;
	for(var i = 0; i < selectedJobNames.length; i++) {
		if(selectedJobNames[i].checked) {
			selected[index++] = selectedJobNames[i].value;
		}
	}

	new Ajax.Request("findVariablesForJob", {
		method: 'get',
		evalJS: false,
		evalJSON: true,
		parameters: {
			names: selected
		},
		onSuccess: function(transport) {
			var responseText = transport.responseText;
			var response = eval('('+responseText+')');

			var ul = $$('#variableNames ul')[0];
			var innerHTML = "";

			for(var i = 0; i < response.result.length; i++) {
				var variableName = response.result[i];
				innerHTML += '<li>'
				innerHTML += variableName
				innerHTML += '<input type="hidden" name="variables" value="'+variableName+'" />';
				innerHTML += '</li>';

			}

			ul.innerHTML = innerHTML;
		},
		onError: function(transport) {
			console.log("Error", transport);
		}
	});
}