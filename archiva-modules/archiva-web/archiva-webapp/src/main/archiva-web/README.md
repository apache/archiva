# ArchivaWeb

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 10.0.1.

## Usage instructions

### Setup environment

You need npm and nodejs, the nodejs version must be at least 10.x or 12.x 

Install ng client:
```shell script
:> nodejs --version
v12.19.0
:> npm --version
6.14.8

npm install -g @angular/cli

cd src/main/archiva-web
npm install

ng --version
Angular CLI: 10.2.0
Node: 12.19.0
OS: linux x64

Angular: 10.2.0
... animations, cli, common, compiler, compiler-cli, core, forms
... platform-browser, platform-browser-dynamic, router
Ivy Workspace: Yes

Package                         Version
---------------------------------------------------------
@angular-devkit/architect       0.1002.0
@angular-devkit/build-angular   0.1002.0
@angular-devkit/core            10.2.0
@angular-devkit/schematics      10.2.0
@schematics/angular             10.2.0
@schematics/update              0.1002.0
rxjs                            6.6.3
typescript                      3.9.6

```
After that, there should be a node_modules directory in the working directory and the ng client
should be runnable.



### Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

### Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

### Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `--prod` flag for a production build.

### Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

### Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).

### Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).

## Information about used components

### I18n-Support
We are using the ngx-translate package for i18n, and not the builtin localization of angular 9.+ . Because, the builtin module
does not allow runtime translations by keys. All keys must exist during compile phase.  



