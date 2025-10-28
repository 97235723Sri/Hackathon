import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { AppComponent } from './app.component';
import { ButtonConfigComponent } from './components/button-config/button-config.component';
import { ControlPanelComponent } from './components/control-panel/control-panel.component';
import { ControlPanelService } from './services/control-panel.service';

const routes: Routes = [
  { path: '', redirectTo: '/control-panel', pathMatch: 'full' },
  { path: 'control-panel', component: ControlPanelComponent },
  { path: 'button-config', component: ButtonConfigComponent }
];

@NgModule({
  declarations: [
    AppComponent,
    ButtonConfigComponent,
    ControlPanelComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule.forRoot(routes)
  ],
  providers: [ControlPanelService],
  bootstrap: [AppComponent]
})
export class AppModule { }
