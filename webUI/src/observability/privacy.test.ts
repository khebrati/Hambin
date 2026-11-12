import { describe, expect, it } from 'vitest';
import { sanitizeErrorEvent } from './privacy';
describe('error capture privacy', () => {
  it('removes private content while retaining readable source locations', () => {
    const result = sanitizeErrorEvent({ request:{data:'private chat'},user:{email:'private@test'},
      breadcrumbs:[{message:'secret'}],exception:{values:[{type:'TypeError',value:'secret',stacktrace:{frames:[
        {filename:'https://watchparty.top/assets/app.js?token=secret',vars:{secret:'value'}},
      ]}}]} });
    expect(result.exception.values[0].stacktrace.frames[0].filename).toBe('https://watchparty.top/assets/app.js');
    expect(JSON.stringify(result)).not.toMatch(/secret|private/);
  });
});
