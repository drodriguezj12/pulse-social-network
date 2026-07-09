import { DatePipe, NgStyle } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { avatarStyle, initialOf } from '../../core/avatar';
import { AuthStore } from '../../stores/auth.store';

@Component({
  selector: 'app-profile',
  imports: [DatePipe, NgStyle],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
})
export class ProfileComponent implements OnInit {
  readonly auth = inject(AuthStore);

  readonly avatarStyle = avatarStyle;
  readonly initialOf = initialOf;

  ngOnInit(): void {
    // Always re-read from GET /users/me so the screen shows live backend data.
    void this.auth.refreshProfile();
  }
}
