import {Component, OnInit} from '@angular/core';
import {AxiosService} from "../../axios.service";
import {Router} from '@angular/router';
import {DataService} from "../../data.service";
import {NgxSpinnerService} from "ngx-spinner";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  constructor(private axiosService: AxiosService,
              private router: Router,
              private data: DataService,
              private spinner: NgxSpinnerService) {
  }

  ngOnInit(): void {
    // Get the user's preferred languages
    const userLangs = navigator.languages;

    // Create a display name object for languages
    const languageNames = new Intl.DisplayNames(['en'], {type: 'language'});

    // Extract and clean the language names and assign the first language name
    this.userLanguage = languageNames.of(userLangs[0]?.split('-')[0]) || 'English';

  }

  active: string = "login";
  username: string = "";
  password: string = "";
  email: string = "";
  firstName: String = "";
  surname: String = "";
  userLanguage: string = "";

  internalization: any = {};

  errorMessages: string[] = [];


  tabSwitch(tab: string): void {
    this.active = tab;
  }

  onSubmitLogin(): void {
    this.spinner.show();
    this.errorMessages = [];
    if (this.username == "" || this.password == "") {
      this.errorMessages.push("Please fill required fields")
      this.spinner.hide();
      return;
    }
    this.axiosService.request(
      "POST",
      "/auth/login",
      {
        "username": this.username,
        "password": this.password
      },
    ).catch((error) => {
      this.spinner.hide();
      this.errorMessages.push(error.response.data.message)
    })
      .then((response) => {
        if (response) {
          localStorage.setItem('user', JSON.stringify(response.data));


          this.data.getInternalizationFromServerWithLanguage(response.data.language).then((internalization) => {
            this.data.getLanguagesFromServer().then((languages) => {
              this.router.navigate(['/']);
            });
          });
        }
      })
  }

  onSubmitRegister() {
    this.spinner.show();
    this.errorMessages = [];
    this.axiosService.request(
      "POST",
      "/auth/register",
      {
        "firstName": this.firstName,
        "surname": this.surname,
        "username": this.username,
        "email": this.email,
        "password": this.password,
        "language": this.userLanguage || "English"
      },
    ).catch((error) => {

      this.spinner.hide().then(r => this.errorMessages.push(error.response.data.message));
    }).then((response) => {
      if (response) {
        this.spinner.hide().then(r => this.active = "login");
      }
    })
  }


}

