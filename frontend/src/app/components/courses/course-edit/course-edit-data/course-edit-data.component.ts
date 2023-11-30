import {Component, HostListener, Input, OnInit} from '@angular/core';
import Editor from "ckeditor5-custom-build";
import {faPlus} from "@fortawesome/free-solid-svg-icons";
import {ActivatedRoute} from "@angular/router";
import {Title} from "@angular/platform-browser";
import {AxiosService} from "../../../../axios.service";
import {DataService} from "../../../../data.service";
import {NgxSpinnerService} from "ngx-spinner";
import {MatDialog} from "@angular/material/dialog";
import {VgApiService} from "@videogular/ngx-videogular/core";


export interface DialogData {
  sectionName: string;
  name: string;
}

@Component({
  selector: 'app-course-edit-data',
  templateUrl: './course-edit-data.component.html',
  styleUrls: ['./course-edit-data.component.css']
})
export class CourseEditDataComponent implements OnInit {
  Editor = Editor;
  faPlus = faPlus;
  user = JSON.parse(localStorage.getItem('user') || '{}');
  course: any = {};
  languages: any;
  isLoading: boolean = true;
  @Input() isEdit!: boolean;
  @Input() courseUrl !: string;

  imageNotFound: string = "https://www.salonlfc.com/wp-content/uploads/2018/01/image-not-found-scaled-1150x647.png";

  courseImage: string = '';
  courseDescription: String = '';
  courseName: String = '';
  courseLanguage: String = '';
  courseCategory: String = '';
  sections: any;
  sectionInputName: string = '';
  videoInputName: string = '';
  selectedFile!: string;

  preload: string = "auto";



  constructor(private route: ActivatedRoute,
              private titleService: Title,
              private axiosService: AxiosService,
              private data: DataService,
              private spinner: NgxSpinnerService,
              public dialog: MatDialog) {

  }

  ngOnInit(): void {
    this.showSpinner();
    this.loadCourseData();
  }

  loadCourseData() {
    if (localStorage.getItem("course-" + this.courseUrl) === null) {
      this.fetchCourseDataFromServer();
    } else {
      this.course = JSON.parse(localStorage.getItem("course-" + this.courseUrl) || '{}');
      this.loadCourseDetails();
    }
  }

  fetchCourseDataFromServer() {
    this.getCourseData(this.courseUrl)
      .then((response) => {
        if (response) {
          //TODO Fix title
          this.titleService.setTitle(response.data.name + " | Course");
          localStorage.setItem("course-" + response.data.url, JSON.stringify(response.data));
          this.course = response.data;
          this.hideSpinner();
          this.loadCourseDetails();
        }
      })
      .catch((error) => {
        console.log(error.response.data.message);
        this.hideSpinner();
      });
  }

  loadCourseDetails() {
    if (!this.userIsAuthor()) {
      window.location.href = '/course/' + this.courseUrl;
    }
    this.hideSpinner();
    this.languages = this.data.getLanguages();
    this.courseImage = this.course.imageUrl || this.imageNotFound;
    this.courseDescription = this.course.description;
    this.courseName = this.course.name;
    this.courseLanguage = this.course.language;
    this.courseCategory = this.course.category;
    this.sections = this.course.sections;
  }

  userIsAuthor(): boolean {
    return this.user.id === this.course.authorId;
  }

  getCourseData(url: string): Promise<any> {
    return this.axiosService.request("GET", "/course/" + url, null);
  }

  addSection() {
    if (this.sectionInputName !== undefined && this.sectionInputName !== "" && this.sectionInputName.replace(/\s/g, '') !== "") {
      this.sections.push({name: this.sectionInputName, videos: []});
    } else {
      alert("Please enter section name");
    }
  }

  showSpinner() {
    this.spinner.show().then(r => console.log("Loading..."));
    this.isLoading = true;
  }

  hideSpinner() {
    this.spinner.hide();
    this.isLoading = false;
  }

  addVideoToSection(name: string) {
    this.sections.forEach((section: any) => {
      /*// add video to section by name of section (section.name) but if section name is "Add section name" then print error
      if (section.name === name && section.name !== "" && section.name.replace(/\s/g, '') !== "") {
        section.videos.push({name: "Add video name", url: ""});
      } else {
        // open modal window with error
        alert("Please change section name before add video");
      }*/
    });
  }

  changeCourseLanguage(language: string) {
    console.log("change lang: " + language);
    this.courseLanguage = language;
  }

  onVideoUploaded(event: any) {
    const selectedFile = event.target.files[0];
    //if selected file is not video mp4,avi,webm then print error
    if (selectedFile) {
      const allowedTypes = ['video/mp4', 'video/webm', 'video/avi'];
      if (!allowedTypes.includes(selectedFile.type)) {
        alert('Please, choose file .avi, .mp4 or .webm.');
        return;
      } else {
        this.selectedFile = URL.createObjectURL(selectedFile);
      }
    }


  }
}