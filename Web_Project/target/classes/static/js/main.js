function login(){
	var email =  $('#loginEmail').val().trim();
	var password = $('#loginPassword').val().trim();
	if(email=="" || password==""){
		alert("Sva polja moraju biti popunjena.")
		return;
	}
	var data = {
			'username':email,
			'password':password
	}
	console.log(data);
	console.log("usao u login()");
	
	
	$.ajax({
		type: 'POST',
        contentType: 'application/json',
        url: 'https://localhost:8081/api/auth/login',
        data: JSON.stringify(data),
        dataType: 'json',
        crossDomain: true,
		cache: false,
		processData: false,
		success:function(response){
			
			var token = response.access_token;
			console.log(token);
			console.log(response);
			
			localStorage.setItem("token",token);
			console.log(email)
			localStorage.setItem("email",email);
			console.log("usao u login() ajax post success")
			$('#loginModal').modal('toggle');
			
			location.reload();
			
		},
		error: function (jqXHR, textStatus, errorThrown) {  
			
			if(jqXHR.status=="401"){
				alert("Pogresan email ili lozinka.");
			}
			
		}
	});
}
function register(){
	var email =  $('#registerEmail').val().trim();
	var password = $('#registerPassword').val().trim();
	console.log(email+" "+password);
	if(email=="" || password==""){
		alert("Sva polja moraju biti popunjena.")
		return;
	}
	var data = {
			'email':email,
			'password':password
	}
	console.log(data);
	
	$.ajax({
		type: 'POST',
        contentType: 'application/json',
        url: 'https://localhost:8081/api/users/register',
        data: JSON.stringify(data),
        dataType: 'json',
        crossDomain: true,
		cache: false,
		processData: false,
		success:function(response){
			$('#registerModal').modal('toggle');
			alert("Uspesna registracija. Da bi mogao da se ulogujes prvo mora admin da ti aktivira nalog.")
		},
		error: function (jqXHR, textStatus, errorThrown) {  
			if(jqXHR.status=="403"){
				alert("Email je vec korisnjen na ovoj stranici.");
			}
		}
	});
	
}

