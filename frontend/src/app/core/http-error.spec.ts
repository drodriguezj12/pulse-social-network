import { HttpErrorResponse } from '@angular/common/http';
import { httpMessage } from './http-error';

describe('httpMessage', () => {
  it('explains upload size errors when the proxy returns 413 without an API body', () => {
    const error = new HttpErrorResponse({
      status: 413,
      error: '<html>Request Entity Too Large</html>',
    });

    expect(httpMessage(error, 'No se pudo crear la publicacion')).toBe(
      'La imagen no puede superar 2MB',
    );
  });
});
